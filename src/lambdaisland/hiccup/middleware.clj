(ns lambdaisland.hiccup.middleware
  (:require
   [clojure.string :as str]
   [lambdaisland.hiccup :as hiccup]))

(defn default-layout
  "Basic layout function used when no specific layout is provided."
  [{:html/keys [head body]}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    head]
   [:body
    body]])

(defn wrap-render
  "Content negotiation for html/hiccup.

  If the client accepts \"text/html\", and the response contains a
  `:html/body` (hiccup form), then use it to render a HTML response. If the
  client asks for a different media type, then the request is passed on for
  other middleware to act upon, e.g. render the `:body` with Muuntaja.

  Additionally `:html/layout` and `:html/head` can be provided, either in the
  response map, or in (Reitit) route data. Layout-fn receives the request map
  plus two additional keys, `:html/body` and `:html/head`, both containing a
  hiccup element or fragment, which should be inserted in the head and body
  elements respectively.

  `:html/head` can be used to set the page title, social media tags, etc.

  The response status if not set defaults to 200.

  Middleware options

  - `:layout-fn` - fallback layout function
  - `:route-data-fn` - function from request map to route data
  - `:accept` - set of mime types that should trigger hiccup rendering
  - `:content-type` - content-type of the response
  "
  ([handler]
   (wrap-render handler nil))
  ([handler {:keys [layout-fn route-data-fn accept content-type]
             :or {layout-fn default-layout
                  route-data-fn (comp :data :reitit.core/match)
                  accept #{"text/html"}
                  content-type "text/html; charset=utf-8"}}]
   (fn [req]
     (let [res           (handler req)
           route-data    (when route-data-fn (route-data-fn req))
           head          (or (:html/head res)
                             (:html/head route-data))
           layout-fn     (first (filter some?
                                        [(:html/layout res)
                                         (:html/layout route-data)
                                         layout-fn]))
           body          (:html/body res)
           accept-header (get-in req [:headers "accept"])]
       ;; Render HTML if there's a `:html/body` key, and the client accepts
       ;; text/html, OR there is no `:body` key, because if there isn't then
       ;; there is nothing else to fall back to.
       (if (and body
                (or (not (:body res))
                    (and accept-header (some accept (str/split accept-header #",")))))
         (-> res
             (assoc :status (or (:status res) 200)
                    :body (hiccup/render (if layout-fn
                                           (layout-fn (assoc req :html/head head :html/body body))
                                           body)))
             (assoc-in [:headers "content-type"] content-type))
         res)))))
