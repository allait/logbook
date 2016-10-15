(ns logbook.test.core
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [logbook.utils :refer [group-prs-by-production-release]]))

(deftest test-group-prs-by-production-release
  (is (= (doall (group-prs-by-production-release [] []))
         nil))

  (is (= (group-prs-by-production-release [{:sha 5} {:sha 6} {:sha 7} {:sha 8} {:sha 9}]
                                          [{:sha 6 :environment "production"} {:sha 8}])
         '(({:sha 9} {:sha 8} {:sha 7}) ({:sha 6} {:sha 5})))))
