
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
           "<div>&lt;p&gt;&lt;/p&gt;</div>"))))
