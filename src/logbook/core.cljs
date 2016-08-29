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

(defonce github-data (r/atom {}))

(def github-api "https://api.github.com")
(def repositories ["allait/test-deployments" "alphagov/digitalmarketplace-api"])

(defn get-pulls [repo]
  (log (format "Requesting %s pull requests" repo))
  (http/get (format "%s/repos/%s/pulls" github-api repo)
            {:with-credentials? false
             :query-params {"state" "all"
                            "per_page" 100}}))

(defn get-deployments [repo]
  (log (format "Requesting %s deployments" repo))
  (http/get (format "%s/repos/%s/deployments" github-api repo)
            {:with-credentials? false
             :query-params {"per_page" 100}}))

(defn list-pulls [data]
  (sort #(compare (:title %1) (:title %2)) (apply concat (map :pulls (vals data)))))

(defonce init
  (go (doseq [repo-name repositories]
    ;; FIXME this is sending requests one at a time
    (let [pulls (<! (get-pulls repo-name))
          deployments (<! (get-deployments repo-name))]
      (swap! github-data assoc-in [repo-name :pulls] (:body pulls))
      (swap! github-data assoc-in [repo-name :deployments] (:body deployments))))))

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
  (fn [{:keys [title url repo user state merged_at]}]
    [:li {:class "pr"}
      [:div.view
       [:div.state state]
       [:div.merged_at merged_at]
       [:img.avatar {:src (:avatar_url user) :width 20 :height 20}]
       [:a {:href url} title]]]))

(defn release-app [props]
  (let [env (r/atom :preview)]
    (fn []
      (let [items (list-pulls @github-data)]
        (log "Items" items)
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
