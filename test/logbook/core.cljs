(ns logbook.test.core
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [logbook.utils :refer [group-deployments]]))

(deftest test-group-deployments
  (is (= (doall (group-deployments [] []))
         ()))

  (is (= (group-deployments [{:created_at 6}] [{:merged_at 5} {:merged_at 7}])
         '({:deployment nil, :pulls ({:merged_at 7})}
           {:deployment {:created_at 6}, :pulls ({:merged_at 5})})))

  (is (= (group-deployments [{:created_at 6}] [{:merged_at 5} {:merged_at 3}])
         '({:deployment {:created_at 6} , :pulls ({:merged_at 5} {:merged_at 3})}))))
