(ns lambdaisland.hiccup
  "Convert Hiccup to clojure.xml style :tag/:attrs/:content maps, with support
  for :<>, function components, and extensible via protocol.

  Anywhere where we need to render Hiccup we should prefer to go through this
  namespace."
  (:require [net.cgrand.enlive-html :as enlive]
            [garden.compiler :as gc]
            [clojure.string :as str]))

(def kebab-case-tags
  ;; from https://github.com/preactjs/preact-compat/issues/222
  #{;; html
    "accept-charset" "http-equiv"
    ;; svg
    "accent-height" "alignment-baseline" "arabic-form" "baseline-shift" "cap-height"
    "clip-path" "clip-rule" "color-interpolation" "color-interpolation-filters"
    "color-profile" "color-rendering" "fill-opacity" "fill-rule" "flood-color"
    "flood-opacity" "font-family" "font-size" "font-size-adjust" "font-stretch"
    "font-style" "font-variant" "font-weight" "glyph-name"
    "glyph-orientation-horizontal" "glyph-orientation-vertical" "horiz-adv-x"
    "horiz-origin-x" "marker-end" "marker-mid" "marker-start" "overline-position"
    "overline-thickness" "panose-1" "paint-order" "stop-color" "stop-opacity"
    "strikethrough-position" "strikethrough-thickness" "stroke-dasharray"
    "stroke-dashoffset" "stroke-linecap" "stroke-linejoin" "stroke-miterlimit"
    "stroke-opacity" "stroke-width" "text-anchor" "text-decoration" "text-rendering"
    "underline-position" "underline-thickness" "unicode-bidi" "unicode-range"
    "units-per-em" "v-alphabetic" "v-hanging" "v-ideographic" "v-mathematical"
    "vert-adv-y" "vert-origin-x" "vert-origin-y" "word-spacing" "writing-mode"
    "x-height"})

(def block-level-tag?
  #{:head :body :meta :title :script :svg :iframe :style
    :link :address :article :aside :blockquote :details
    :dialog :dd :div :dl :dt :fieldset :figure :figcaption :footer :form
    :h1 :h2 :h3 :h4 :h5 :h6 :header :hgroup :hr :li :main :nav :ol :p :pre :section :table :ul})

(defn- attr-map? [node-spec]
  (and (map? node-spec) (not (keyword? (:tag node-spec)))))

(defn- kebab-in-html? [attr-str]
  (or (contains? kebab-case-tags attr-str)
      (str/starts-with? attr-str "data-")
      (str/starts-with? attr-str "aria-")
      (str/starts-with? attr-str "hx-")))

(defn- kebab->camel [s]
  (str/replace s #"-(\w)" (fn [[_ match]] (str/capitalize match))))

(defn- camel->kebab [s]
  (str/replace s #"([A-Z])" (fn [[_ match]] (str "-" (str/lower-case match)))))

(defn convert-attribute-reagent-logic
  [attr]
  (cond
    (string? attr)
    attr
    (keyword? attr)
    (if (kebab-in-html? (name attr))
      (name attr)
      (kebab->camel (name attr)))))

(defn convert-attribute-react-logic
  [attr-str]
  (let [kebab-str (camel->kebab attr-str)]
    ;; not using kebab-in-html? here because
    ;; React does not convert dataFoo to data-foo
    ;; but does convert fontStretch to font-stretch
    (if (kebab-case-tags kebab-str)
      kebab-str
      attr-str)))

(defn convert-attribute [attr]
  (->> attr
       convert-attribute-reagent-logic
       convert-attribute-react-logic))

(defn- nodify [node-spec {:keys [newlines?] :as opts}]
  (cond
    (string? node-spec) node-spec
    (vector? node-spec)
    (let [[tag & [m & ms :as more]] node-spec]
      (cond
        (= ::unsafe-html tag)
        ^{::enlive/annotations {:emit (fn [_ t] (apply enlive/append! t more))}}
        {:tag ::unsafe-html
         :content more}

        (= :<> tag)
        (enlive/flatmap #(nodify % opts) more)

        (or (keyword? tag) (symbol? tag) (string? tag))
        (let [[tag-name & segments] (.split (name tag) "(?=[#.])")
              id (some (fn [^String seg]
                         (when (= \# (.charAt seg 0)) (subs seg 1))) segments)
              classes (keep (fn [^String seg]
                              (when (= \. (.charAt seg 0)) (subs seg 1)))
                            segments)
              node {:tag (keyword tag-name)
                    :attrs (if (attr-map? m)
                             (into {} (filter val m))
                             {})
                    :content (enlive/flatmap #(nodify % opts) (if (attr-map? m) ms more))}
              node (update node :attrs
                           (fn [attrs]
                             (->> attrs
                                  (map (fn [[k v]]
                                         [(convert-attribute k) v]))
                                  (into {}))))
             node (if (and id (not (contains? (:attrs node) :id))) 
                     (assoc-in node [:attrs :id] id) node)
              node (if (seq classes)
                     (update-in node
                                [:attrs "class"]
                                (fn [kls]
                                  (concat classes (if (string? kls) [kls] kls))))
                     node)]
          (cond-> node
            (map? (get-in node [:attrs "style"]))
            (update-in [:attrs "style"] (fn [style]
                                          (-> (gc/compile-css [:& style])
                                              (str/replace #"^\s*\{|\}\s*$" "")
                                              str/trim)))
            (sequential? (get-in node [:attrs "class"]))
            (update-in [:attrs "class"] #(str/join " " %))
            (and newlines? (block-level-tag? tag))
            (->> (list "\n"))))

        (or (fn? tag) (= :lambdaisland.ornament/styled (type tag)))
        (nodify (apply tag more) opts)

        :else
        (throw (ex-info "Not a valid hiccup tag" {:tag tag :form node-spec}))))
    (sequential? node-spec) (enlive/flatmap #(nodify % opts) node-spec)
    (map? node-spec) (update-in node-spec [:content] (comp #(nodify % opts) seq))
    :else (str node-spec)))

;; TODO: this isn't super clean since we want :newlines? to be opt-in, but we
;; don't have a way to pass options here because it's varargs. At the same time
;; we mainly use this with a single arg, and also the name isn't great,
;; something less generic would be nice... so this API call could stand some
;; rethinking

(defn html
  "Convert hiccup to clojure.xml.
  Like net.cgrand.enlive/html, but additionally support function components,
  fragments with :<>, and extensible via the HiccupTag protocol."
  [& nodes-specs]
  (enlive/flatmap #(nodify % {:newlines? true}) nodes-specs))

;;https://www.w3.org/TR/html5/syntax.html#void-elements
(def html5-void-elements
  #{:area :base :br :col :embed :hr :img :input :keygen :link :meta :param
    :source :track :wbr})

(defn render-html*
  "Render clojure.xml to string.
  Emits void-tags for HTML5 void elements (no closing tag or self-closing)."
  [h]
  (with-redefs [enlive/self-closing-tags html5-void-elements]
    (apply str (enlive/emit* h))))

(defn render-html
  "Render clojure.xml to string, add DOCTYPE"
  [h]
  (str "<!DOCTYPE html>\n" (render-html* h)))

(defn render
  "Render hiccup to string"
  ([h]
   (render h nil))
  ([h {:keys [doctype?]
       :or {doctype? true}
       :as opts}]
   ((if doctype? render-html render-html*) (nodify h opts))))
