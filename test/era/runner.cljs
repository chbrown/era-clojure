(ns era.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [era.test]))

(doo-tests 'era.test)
