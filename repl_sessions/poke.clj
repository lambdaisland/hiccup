(ns lambdaisland-hiccup-poke
  (:require [lambdaisland.hiccup :as h]))

(h/render [:div
           {:data-a 123}]
          {:doctype? false})
