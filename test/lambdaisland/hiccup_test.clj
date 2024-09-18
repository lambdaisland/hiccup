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
    (let [style {:color "blue" :border "black"}]
      (is (= (hiccup/render [:div {:style style} [:p]] {:doctype? false})
           "<div style=\"color: blue; border: black;\"><p></p></div>"))))
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
