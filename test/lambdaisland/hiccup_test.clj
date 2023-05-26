
(ns lambdaisland.hiccup-test
  (:require [clojure.test :refer [deftest testing is]]
            [lambdaisland.hiccup :as hiccup]))

(defn my-test-component [contents]
  [:p contents])

(defn test-fragment-component [contents]
  [:<>
   [:p contents]
   [:p contents]])

(deftest render-test
  (testing "simple tag"
    (is (= (hiccup/render [:p] {:doctype? false})
         "<p></p>")))
  (testing "nested tags"
    (is (= (hiccup/render [:div [:p]] {:doctype? false})
           "<div><p></p></div>")))
  (testing "styled tag"
    (is (= (hiccup/render [:div {:style {:color "blue"}} [:p]] {:doctype? false})
           "<div style=\"color: blue;\"><p></p></div>")))
  (testing "simple component"
    (is (= (hiccup/render [my-test-component "hello"] {:doctype? false})
           "<p>hello</p>")))
  (testing "simple component with fragment"
    (is (= (hiccup/render [:div [test-fragment-component "hello"]] {:doctype? false})
           "<div><p>hello</p><p>hello</p></div>")))
  (testing "pre-rendered HTML"
    (is (= (hiccup/render [::hiccup/unsafe-html "<body><main><article><p></p></article></main></body>"] {:doctype? false})
           "<body><main><article><p></p></article></main></body>")))
  (testing "autoescaping"
    (is (= (hiccup/render [:div "<p></p>"] {:doctype? false})
           "<div>&lt;p&gt;&lt;/p&gt;</div>")))

  (testing "classes in tags"
    (is (= (hiccup/render [:div.foo] {:doctype? false})
           "<div class=\"foo\"></div>"))

    (is (= (hiccup/render [:div.foo.bar] {:doctype? false})
           "<div class=\"foo bar\"></div>"))

    (is (= (hiccup/render [:div.foo {:class "bar"}] {:doctype? false})
           "<div class=\"foo bar\"></div>"))

    (is (= (hiccup/render [:div.foo {:class "foo"}] {:doctype? false})
           "<div class=\"foo foo\"></div>")))

  (testing "attribute conversion"
    ;; convert kebab-case and camelCase attributes
    ;; based on behaviour of using Reagent + React
    ;; except, don't force lowercase (the * below)
    (doseq [[input expected]
            {"tabIndex" "tabIndex" ;; *
             "dataA" "dataA"
             "fontStyle" "font-style"

             "tab-index" "tab-index"
             "data-b" "data-b"
             "font-variant" "font-variant"

             :tabIndex "tabIndex" ;; *
             :dataD "dataD"
             :fontStretch "font-stretch"

             :tab-index "tabIndex" ;; *
             :data-c "data-c"
             :font-weight "font-weight"

             :hx-foo "hx-foo"
             "hx-foo" "hx-foo"}]
      (testing (str (pr-str input) "->" (pr-str expected))
        (is (= expected
               (hiccup/convert-attribute input)))
        (is (= (str "<div " expected "=\"baz\"></div>")
               (hiccup/render [:div {input "baz"}]
                              {:doctype? false})))))))
;borrowed from Hiccup:

(defmacro html [& body]
  `(hiccup/render ~@body {:doctype? false})
  )

(macroexpand-1 '(html [:test]))

(deftest tag-names
  (testing "basic tags"
    (is (= (str (html [:div])) "<div></div>"))
    (is (= (str (html ["div"])) "<div></div>"))
    (is (= (str (html ['div])) "<div></div>")))
  (testing "tag syntax sugar"
    (is (= (str (html [:div#foo])) "<div id=\"foo\"></div>"))
    (is (= (str (html [:div.foo])) "<div class=\"foo\"></div>"))
    (is (= (str (html [:div.foo (str "bar" "baz")]))
           "<div class=\"foo\">barbaz</div>"))
    (is (= (str (html [:div.a.b])) "<div class=\"a b\"></div>"))
    (is (= (str (html [:div.a.b.c])) "<div class=\"a b c\"></div>"))
    (is (= (str (html [:div#foo.bar.baz]))
           "<div id=\"foo\" class=\"bar baz\"></div>"))))

(deftest tag-contents
  (testing "empty tags"
    (is (= (str (html [:div])) "<div></div>"))
    (is (= (str (html [:h1])) "<h1></h1>"))
    (is (= (str (html [:script])) "<script></script>"))
    (is (= (str (html [:text])) "<text></text>"))
    (is (= (str (html [:a])) "<a></a>"))
    (is (= (str (html [:iframe])) "<iframe></iframe>"))
    (is (= (str (html [:title])) "<title></title>"))
    (is (= (str (html [:section])) "<section></section>"))
    (is (= (str (html [:select])) "<select></select>"))
    (is (= (str (html [:object])) "<object></object>"))
    (is (= (str (html [:video])) "<video></video>")))
  (testing "void tags"
    (is (= (str (html [:br])) "<br />"))
    (is (= (str (html [:link])) "<link />"))
    (is (= (str (html [:colgroup {:span 2}])) "<colgroup span=\"2\"></colgroup>"))
    (is (= (str (html [:colgroup [:col]])) "<colgroup><col /></colgroup>")))
  (testing "tags containing text"
    (is (= (str (html [:text "Lorem Ipsum"])) "<text>Lorem Ipsum</text>")))
  (testing "contents are concatenated"
    (is (= (str (html [:body "foo" "bar"])) "<body>foobar</body>"))
    (is (= (str (html [:body [:p] [:br]])) "<body><p></p><br /></body>")))
  (testing "seqs are expanded"
    (is (= (str (html [:body (list "foo" "bar")])) "<body>foobar</body>"))
    (is (= (str (html (list [:p "a"] [:p "b"]))) "<p>a</p><p>b</p>")))
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? clojure.lang.ExceptionInfo
                 (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (is (= (str (html [:div [:p]])) "<div><p></p></div>"))
    (is (= (str (html [:div [:b]])) "<div><b></b></div>"))
    (is (= (str (html [:p [:span [:a "foo"]]]))
           "<p><span><a>foo</a></span></p>"))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (is (= (str (html [:xml {}])) "<xml></xml>")))
  (testing "tag with populated attribute map"
    (is (= (str (html [:xml {:a "1", :b "2"}])) "<xml a=\"1\" b=\"2\"></xml>"))
    (is (= (str (html [:img {"id" "foo"}])) "<img id=\"foo\" />"))
    (is (= (str (html [:img {'id "foo"}])) "<img id=\"foo\" />"))
    (is (= (str (html [:xml {:a "1", 'b "2", "c" "3"}]))
           "<xml a=\"1\" b=\"2\" c=\"3\"></xml>")))
  (testing "attribute values are escaped"
    (is (= (str (html [:div {:id "\""}])) "<div id=\"&quot;\"></div>")))
  (testing "boolean attributes"
    #_(is (= (str (html [:input {:type "checkbox" :checked true}]))
           "<input type=\"checkbox\" checked=\"checked\" />"))
    (is (= (str (html [:input {:type "checkbox" :checked false}]))
           "<input type=\"checkbox\" />")))
  (testing "nil attributes"
    (is (= (str (html [:span {:class nil} "foo"]))
           "<span>foo</span>")))
  (testing "vector attributes"
    (is (= (str (html [:span {:class ["bar" "baz"]} "foo"]))
           "<span class=\"bar baz\">foo</span>"))
    (is (= (str (html [:span {:class ["baz"]} "foo"]))
           "<span class=\"baz\">foo</span>"))
    (is (= (str (html [:span {:class "baz bar"} "foo"]))
           "<span class=\"baz bar\">foo</span>")))
  (testing "map attributes"
    (is (= (str (html [:span {:style {:background-color :blue, :color "red",
                                      :line-width 1.2, :opacity "100%"}} "foo"]))
           "<span style=\"background-color: blue;\n  color: red;\n  line-width: 1.2;\n  opacity: 100%;\">foo</span>"))) ;format tweaked from original to match our format
  (testing "resolving conflicts between attributes in the map and tag"
    (is (= (str (html [:div.foo {:class "bar"} "baz"]))
           "<div class=\"foo bar\">baz</div>"))
    (is (= (str (html [:div.foo {:class ["bar"]} "baz"]))
           "<div class=\"foo bar\">baz</div>"))
    (is (= (str (html [:div#bar.foo {:id "baq"} "baz"]))
           "<div id=\"baq\" class=\"foo\">baz</div>")))) ;swapped order from original test
