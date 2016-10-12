(ns logbook.test.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [logbook.test.core]))

(doo-tests 'logbook.test.core)
