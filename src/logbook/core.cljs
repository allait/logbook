(ns logbook.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn format [& args]
  (apply gstring/format args))

(defn log [message & args]
  (apply js/console.log (cons message (map pr-str args))))


(defonce repos (r/atom {}))

(def github-api "https://api.github.com")

(defn get-pulls [repo]
  (log "Reloading")
  (http/get (format "%s/repos/%s/pulls" github-api repo)
            {:with-credentials? false
             :query-params {"state" "all"
                            "per_page" 100}}))

(defonce init (do
              (go (let [response (<! (get-pulls "allait/test-deployments"))]
                    (swap! repos assoc-in
                           ["allait/test-deployments" :pulls] (:body response))))))

(defn release-stats [{:keys [total env]}]
  (let [props-for (fn [name]
                    {:class (if (= name @env) "selected")
                     :on-click #(reset! env name)})]
    [:div
     [:ul#filters
      [:li [:a (props-for :preview) "Preview"]]
      [:li [:a (props-for :staging) "Staging"]]
      [:li [:a (props-for :production) "Production"]]]]))

(defn release-item []
  (fn [{:keys [title]}]
    [:li {:class "pull"}
      [:div.view
      [:span title]]]))

(defn release-app [props]
  (let [env (r/atom :preview)]
    (fn []
      (let [items (:pulls (@repos "allait/test-deployments"))]
        [:div
         [:section#releases
          [:header#header
           [:h1 "Releases"]]
          (if (-> items count pos?)
            [:div
             [:section#main
              [:ul#releases
               (for [item items]
                 ^{:key (:id item)} [release-item item])]]
             [:footer#footer
              [release-stats {:total (count items) :env env}]]]
            [:div [:p "Loading Github data"]])]]))))

(defn ^:export run []
  (r/render [release-app]
            (js/document.getElementById "app")))
(run)
