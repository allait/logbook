(ns logbook.utils)


(defn is-deployment? [deployment-or-pull]
  (not (contains? deployment-or-pull :merged_at)))

(defn object-timestamp [deployment-or-pull]
  (or (:merged_at deployment-or-pull) (:created_at deployment-or-pull)))

(defn merge-deployments-and-pulls [deployments pulls]
  (let [merged (partition-by
                 is-deployment?
                 (reverse (sort-by object-timestamp (concat deployments pulls))))]
    (if (is-deployment? (first (first merged)))
      merged
      (cons '(nil) merged))))

(defn group-deployments [deployments pulls]
  (for
    [[d p] (partition 2 (merge-deployments-and-pulls deployments pulls))]
    {:deployment (first d) :pulls p}))
