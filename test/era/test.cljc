(ns era.test
  (:require [clojure.test #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
            [era.core :as era]
            [era.format :as format]))

;; era.core

(deftest test-now
  (is (not (nil? (era/now)))))

(def coercions
  {#?(:clj java.time.OffsetDateTime :cljs js/Date) era/->OffsetDateTime
   #?(:clj java.time.ZonedDateTime  :cljs js/Date) era/->ZonedDateTime
   #?(:clj java.time.Instant        :cljs js/Date) era/->Instant
   #?(:clj java.sql.Timestamp       :cljs js/Date) era/->Timestamp
   #?(:clj java.util.Date           :cljs js/Date) era/->Date})

(def date #inst "2001-02-03T04:05:06.007Z")

(def dates
  (map #(% date) (vals coercions)))

(deftest test-cross
  (doseq [date dates
          [type coercion] coercions]
    (is (instance? type (coercion date)))))

(deftest test-strings
  (testing "parsing OffsetDateTime"
    (is (era/inst= date (era/->OffsetDateTime "2001-02-03T04:05:06.007-00:00"))))
  (testing "parsing ZonedDateTime"
    (is (era/inst= date (era/->ZonedDateTime "2001-02-02T22:05:06.007-06:00[America/Chicago]"))))
  (testing "parsing Instant"
    (is (era/inst= date (era/->Instant "2001-02-03T04:05:06.007Z"))))
  (testing "parsing Timestamp"
    (is (era/inst= date (era/->Timestamp "2001-02-03T04:05:06.007Z"))))
  (testing "parsing Date"
    (is (era/inst= date (era/->Date "2001-02-03T04:05:06.007Z")))))

(deftest test-nils
  (doseq [[_ coercion] coercions]
    (is (nil? (coercion nil)))))

; add

(deftest test-add
  (testing "adding month to OffsetDateTime"
    (is (era/inst= date (era/add (era/->OffsetDateTime "2001-01-03T04:05:06.007-00:00")
                                 {:months 1}))))
  (testing "adding numeric seconds to OffsetDateTime"
    (is (era/inst= date (era/add (era/->OffsetDateTime "2001-02-03T04:05:01.007-00:00") 5))))
  (testing "adding multiple components to OffsetDateTime"
    (is (era/inst= date (era/add (era/->OffsetDateTime "1999-11-30T00:00:00.007-00:00")
                                 {:seconds 6 :minutes 5 :hours 4 :days 3 :months 2 :years 1}))))
  #?(:clj
    (testing "adding TemporalAmount to OffsetDateTime"
      (is (era/inst= date (era/add (era/->OffsetDateTime "2001-01-01T04:05:06.007-00:00")
                                   (java.time.Period/of 0 1 2)))))))

;; era.format

(def locales
  {:full   "Friday, February 2, 2001 10:05:06 PM CST"
   :long   "February 2, 2001 10:05:06 PM CST"
   :medium "Feb 2, 2001 10:05:06 PM"
   :short  "2/2/01 10:05 PM"})

(deftest test-format-iso
  (doseq [date dates]
    (is (= "2001-02-03T04:05:06.007Z" (format/iso date)))))

(deftest test-format-locale
  (doseq [date dates
          [style result] locales]
    (testing style
      (is (= result (format/locale date style))))))

(deftest test-format-locale-implicit-style
  (doseq [date dates]
    (is (= (:medium locales) (format/locale date)))))

(deftest test-format-locale-missing-style
  (is (thrown? #?(:clj java.lang.AssertionError :cljs js/Error) (format/locale date :missing))))
