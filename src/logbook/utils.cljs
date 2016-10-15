(ns logbook.utils
  (:require [clojure.string]))

(defn pr-map [github-pr]
  {:id (:id github-pr)
   :number (:number github-pr)
   :repo (get-in github-pr [:base :repo :url])
   :title (:title github-pr)
   :sha (:merge_commit_sha github-pr)
   :url (clojure.string/replace (clojure.string/replace (:url github-pr) #"(api\.|repos/)" "")
                                #"/pulls/" "/pull/")
   :avatar (:avatar_url (github-pr :user))
   :state (if (:merged_at github-pr) "merged" (:state github-pr))
   :timestamp (or (:merged_at github-pr) (:created_at github-pr))})

(defn release-map [github-release]
  {:id (:id github-release)
   :repo (:repository_url github-release)
   :environment (:environment github-release)
   :sha (:sha github-release)
   :timestamp (or (:created_at (:payload github-release)) (:created_at github-release))
   :ref (:ref github-release)})

(defn group-production-releases [releases]
  (group-by :sha (filter #(= (:environment %1) "production") releases)))

(defn sort-prs [prs]
  (reverse (sort-by #(vec (map % [:state :timestamp])) (filter #(not= (:state %1) "closed") prs))))

(defn partition-when [pred? coll]
  (when-let [s (seq coll)]
    (let [run (if (pred? (first s))
                (cons (first s) (take-while (complement pred?) (next s)))
                (take-while (complement pred?) s))]
      (cons run (partition-when pred? (drop (count run) coll))))))

(defn group-prs-by-production-release [prs releases]
  (partition-when #(contains? (group-production-releases releases) (:sha %1)) (sort-prs prs)))
