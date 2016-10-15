(ns logbook.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljsjs.moment]
            [logbook.utils :refer [pr-map release-map group-prs-by-production-release]]))

(enable-console-print!)

(defn format [& args]
  (apply gstring/format args))

(defn log [message & args]
  (apply js/console.log (cons message (map pr-str args))))

(defonce github-data (r/atom {}))

(def github-api "https://api.github.com")
(def repositories ["alphagov/digitalmarketplace-buyer-frontend" "alphagov/digitalmarketplace-api"])

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
  ;; FIXME this is sending requests fori on repo at a time
  (go (doseq [repo-name repositories]
    (let [pulls (get-pulls repo-name)
          deployments (get-deployments repo-name)]
        (swap! github-data assoc-in [repo-name :pulls]
               (map pr-map (:body (<! pulls))))
        (swap! github-data assoc-in [repo-name :deployments]
               (map release-map (:body (<! deployments))))))))

(defn release-stats [{:keys [total env]}]
  (let [props-for (fn [name]
                    {:class (if (= name @env) "selected")
                     :on-click #(reset! env name)})]
    [:div
     [:ul#filters
      [:li [:a (props-for :preview) "Preview"]]
      [:li [:a (props-for :staging) "Staging"]]
      [:li [:a (props-for :production) "Production"]]]]))

(defn pr-item [pr releases-map]
  [:li.item
    [:ul.releases
     (let [releases (releases-map (:sha pr))
           first-ts (js/moment (:timestamp (first releases)))]
        (for [release releases]
          (let [ts (js/moment (release :timestamp))]
            ^{:key (:id release)}
            [:li.release {:class (:environment release)}
             (if (= release (first releases))
               [:span.release-time {:title (.format ts "dddd, MMMM Do YYYY, h:mm:ss a")}
                (.format ts "dddd, MMM Do, HH:mm:ss")]
               [:span.release-time {:title (.format ts "dddd, MMMM Do YYYY, h:mm:ss a")}
                (.from ts first-ts true) " earlier"]
               )])))]
    [:div.pr
      [:img.avatar {:src (:avatar pr)}]
      [:div.app (:app pr)]
      [:a.title {:href (pr :url) :target "_blank" :rel "noopener"}
       (format "%s (#%s)" (pr :title) (pr :number))]
      (let [ts (js/moment (pr :timestamp))]
        [:div.pr-time {:title (.format ts "dddd, MMMM Do YYYY, h:mm:ss a")}
        (.format ts "dddd, MMM Do, HH:mm:ss")])
      [:div.state (pr :state)]]])

(defn release-app [props]
  (let [env (r/atom :preview)]
    (fn []
      (let [releases (:deployments (@github-data "alphagov/digitalmarketplace-api"))
            releases-map (group-by :sha releases)
            prs (get-in @github-data ["alphagov/digitalmarketplace-api" :pulls])
            items (group-prs-by-production-release prs releases)]
        [:div
          [:header#header
           [:h1 "Releases"]]
          (if (-> items count pos?)
            [:section#main
              [:ul.prs
                (for [item (flatten items)]
                  ^{:key (:id item)} [pr-item item releases-map])]]
            [:div [:p "Loading Github data"]])
          [:footer#footer
            [release-stats {:total (count items) :env env}]]]))))

(defn ^:export run []
  (r/render [release-app] (js/document.getElementById "app")))

(run)
