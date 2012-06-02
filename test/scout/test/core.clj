(ns scout.test.core
  (:use clojure.test)
  (:require [scout.core :as scout]))

;;
;; Information access tests.
;;

(deftest test-position
  (is (= 0 (scout/position (scout/scanner ""))))
  (is (= 0 (scout/position (scout/scanner "test"))))
  (is (= 1 (scout/position (scout.core.Scanner. "test" 1 nil)))))

(deftest test-beginning-of-line?
  (is (= true (scout/beginning-of-line? (scout/scanner ""))))
  (is (= true (scout/beginning-of-line? (scout/scanner "test"))))
  (is (= false (scout/beginning-of-line?
                (scout.core.Scanner. "test\r\ntest" 5 nil))))
  (is (= true (scout/beginning-of-line?
               (scout.core.Scanner. "test\r\ntest" 6 nil)))))

(deftest test-end?
  (is (= true (scout/end? (scout/scanner ""))))
  (is (= false (scout/end? (scout/scanner "test")))))

(deftest test-remainder
  (is (= "test" (scout/remainder (scout/scanner "test"))))
  (is (= "" (scout/remainder (scout/scanner ""))))
  (is (= "" (scout/remainder (scout.core.Scanner. "test" 4 nil))))
  (is (= "" (scout/remainder (scout.core.Scanner. "test" 5 nil)))))

(deftest test-groups
  (is (= ["m"]
         (scout/groups (scout/scanner "test" 0
                                      (scout/match-info 0 1 ["m"]))))))

(deftest test-matched
  (is (= "m"
         (scout/matched (scout/scanner "test" 0
                                       (scout/match-info 0 1 ["m"]))))))

(deftest test-pre-match
  (is (= "beginn"
         (scout/pre-match (scout/scanner "beginning" 9
                                         (scout/match-info 6 8 ["in"])))))
  (is (= "test"
         (-> (scout/scanner "test string")
             (scout/scan #"test")
             (scout/scan #"\s+")
             scout/pre-match))))

(deftest test-post-match
  (is (= "ning"
         (scout/post-match (scout/scanner "beginning" 5
                                          (scout/match-info 3 5 ["in"])))))
  (is (= "string"
         (-> (scout/scanner "test string")
             (scout/scan #"test")
             (scout/scan #"\s+")
             scout/post-match))))

;;
;; Scanning/Advancing tests.
;;

(deftest test-scan
  (is (= "t"
         (-> (scout/scanner "test")
             (scout/scan #"t")
             scout/matched)))
  (is (= 1 (-> (scout/scanner "test")
               (scout/scan #"t")
               scout/position)))
  (is (= "test"
         (-> (scout/scanner "test")
             (scout/scan #"test")
             scout/matched)))
  (is (scout/end? (scout/scan (scout/scanner "test") #"test")))
  (is (= ["t"]
         (-> (scout/scanner "test-string")
             (scout/scan #"t")
             scout/groups)))
  ;; Compounded scans should work.
  (is (= 5
         (-> (scout/scanner "test string")
             (scout/scan #"test")
             (scout/scan #"\s+")
             scout/position)))
  (is (= 4
         (-> (scout/scanner "test string")
             (scout/scan #"test")
             (scout/scan #"\s+")
             (get-in [:match :start]))))
  (is (= 5
         (-> (scout/scanner "test string")
             (scout/scan #"test")
             (scout/scan #"\s+")
             (get-in [:match :end]))))
  ;; Failing to match shoud leave us in the same position
  (is (= 0 (scout/position (scout/scan (scout/scanner "testgoal")
                                       #"notinthestring"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (-> (scout/scanner "test string")
                 (scout/scan #"test")
                 (scout/scan #"notinthestring")
                 (get :match)))))

(deftest test-scan-until
  (is (= "goal" (-> (scout/scanner "testgoal")
                    (scout/scan-until #"goal")
                    scout/matched)))
  (is (= 8 (-> (scout/scanner "testgoal")
               (scout/scan-until #"goal")
               scout/position)))
  (is (= "goal"
         (-> (scout/scanner "goal")
             (scout/scan-until #"goal")
             scout/matched)))
  (is (scout/end? (scout/scan-until (scout/scanner "goal") #"goal")))
  (is (scout/end? (scout/scan-until (scout/scanner "testgoal") #"goal")))
  (is (= ["s"] (-> (scout/scanner "test string")
                   (scout/scan-until #"s")
                   scout/groups)))
  ;; Compounded scan-untils should work.
  (is (= 8 (-> (scout/scanner "test string")
               (scout/scan-until #"s")
               (scout/scan-until #"r")
               scout/position)))
  (is (= 7 (-> (scout/scanner "test string")
               (scout/scan-until #"s")
               (scout/scan-until #"r")
               (get-in [:match :start]))))
  (is (= 8 (-> (scout/scanner "test-string")
               (scout/scan-until #"s")
               (scout/scan-until #"r")
               (get-in [:match :end]))))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (-> (scout/scanner "testgoal")
               (scout/scan-until #"notinthestring")
               scout/position)))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (-> (scout/scanner "test string")
                 (scout/scan #"test")
                 (scout/scan-until #"notinthestring")
                 (get :match)))))

(deftest test-skip-to-match-start
  (is (= "goal"
         (-> (scout/scanner "testgoal")
             (scout/skip-to-match-start #"goal")
             scout/matched)))
  (is (= 4 (-> (scout/scanner "testgoal")
               (scout/skip-to-match-start #"goal")
               scout/position)))
  (is (= "goal"
         (-> (scout/scanner "goal")
             (scout/skip-to-match-start #"goal")
             scout/matched)))
  ;; Calling scan on result of skip-to-match-start should work.
  (is (= "goal"
         (-> (scout/scanner "testgoal")
             (scout/skip-to-match-start #"goal")
             (scout/scan #"goal")
             scout/matched)))
  (is (-> (scout/scanner "testgoal")
          (scout/skip-to-match-start #"goal")
          (scout/scan #"goal")
          scout/end?))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (-> (scout/scanner "testgoal")
               (scout/skip-to-match-start #"yes")
               scout/position)))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (-> (scout/scanner "test string")
                 (scout/scan #"test")
                 (scout/skip-to-match-start #"notinthestring")
                 (get :match)))))

;;
;; Look-ahead tests.
;;

(deftest test-check
  (is (= "t"
         (scout/check (scout/scanner "test") #"t")))
  (is (= "test"
         (scout/check (scout/scanner "test") #"test"))))

(deftest test-check-until
  (is (= "goal"
         (scout/check-until (scout/scanner "testgoal") #"goal")))
  (is (= "goal"
         (scout/check-until (scout/scanner "goal") #"goal"))))

(deftest test-check-until-inclusive
  (is (= "testgoal"
         (scout/check-until-inclusive (scout/scanner "testgoal") #"goal")))
  (is (= "goal"
         (scout/check-until-inclusive (scout/scanner "goal") #"goal"))))

(deftest test-peek ;; Renamed to peep here.
  (is (= "t"
         (scout/peek (scout/scanner "test") 1)))
  (is (= "test"
         (scout/peek (scout/scanner "test") 4)))
  (is (= "test"
         (scout/peek (scout/scanner "test") 500))))