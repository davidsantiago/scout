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
         (scout/pre-match (scout/scan (scout/scan (scout/scanner "test string")
                                                  #"test")
                                      #"\s+")))))

(deftest test-post-match
  (is (= "ning"
         (scout/post-match (scout/scanner "beginning" 5
                                          (scout/match-info 3 5 ["in"])))))
  (is (= "string"
         (scout/post-match (scout/scan (scout/scan (scout/scanner "test string")
                                                   #"test")
                                       #"\s+")))))

;;
;; Scanning/Advancing tests.
;;

(deftest test-scan
  (is (= "t"
         (scout/matched (scout/scan (scout/scanner "test") #"t"))))
  (is (= 1 (scout/position (scout/scan (scout/scanner "test") #"t"))))
  (is (= "test"
         (scout/matched (scout/scan (scout/scanner "test") #"test"))))
  (is (scout/end? (scout/scan (scout/scanner "test") #"test")))
  (is (= ["t"]
           (scout/groups (scout/scan (scout/scanner "test string") #"t"))))
  ;; Compounded scans should work.
  (is (= 5
         (scout/position (scout/scan (scout/scan (scout/scanner "test string")
                                                 #"test")
                                     #"\s+"))))
  (is (= 4
         (:start (:match (scout/scan (scout/scan (scout/scanner "test string")
                                                 #"test")
                                     #"\s+")))))
  (is (= 5
         (:end (:match (scout/scan (scout/scan (scout/scanner "test string")
                                               #"test")
                                   #"\s+")))))
  ;; Failing to match shoud leave us in the same position
  (is (= 0 (scout/position (scout/scan (scout/scanner "testgoal")
                                       #"notinthestring"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (scout/scan (scout/scan (scout/scanner "test string")
                                             #"test")
                                 #"notinthestring")))))

(deftest test-scan-until
  (is (= "goal"
         (scout/matched (scout/scan-until (scout/scanner "testgoal")
                                          #"goal"))))
  (is (= 8 (scout/position (scout/scan-until (scout/scanner "testgoal")
                                             #"goal"))))
  (is (= "goal"
         (scout/matched (scout/scan-until (scout/scanner "goal") #"goal"))))
  (is (scout/end? (scout/scan-until (scout/scanner "goal") #"goal")))
  (is (scout/end? (scout/scan-until (scout/scanner "testgoal") #"goal")))
  (is (= ["s"]
         (scout/groups (scout/scan-until (scout/scanner "test string")
                                         #"s"))))
  ;; Compounded scan-untils should work.
  (is (= 8
         (scout/position (scout/scan-until (scout/scan-until (scout/scanner "test string")
                                                             #"s")
                                           #"r"))))
  (is (= 7
         (:start (:match (scout/scan-until (scout/scan-until (scout/scanner "test string")
                                                             #"s")
                                           #"r")))))
  (is (= 8
         (:end (:match (scout/scan-until (scout/scan-until (scout/scanner "test-string")
                                                           #"s")
                                         #"r")))))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (scout/position (scout/scan-until (scout/scanner "testgoal")
                                             #"notinthestring"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (scout/scan-until (scout/scan (scout/scanner "test string")
                                                   #"test")
                                       #"notinthestring")))))

(deftest test-skip-to-match-start
  (is (= "goal"
         (scout/matched (scout/skip-to-match-start (scout/scanner "testgoal")
                                                   #"goal"))))
  (is (= 4 (scout/position (scout/skip-to-match-start (scout/scanner "testgoal")
                                                     #"goal"))))
  (is (= "goal"
         (scout/matched (scout/skip-to-match-start (scout/scanner "goal")
                                                   #"goal"))))
  ;; Calling scan on result of skip-to-match-start should work.
  (is (= "goal"
         (scout/matched (scout/scan (scout/skip-to-match-start (scout/scanner "testgoal")
                                                               #"goal")
                                    #"goal"))))
  (is (scout/end? (scout/scan (scout/skip-to-match-start (scout/scanner "testgoal")
                                                         #"goal")
                              #"goal")))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (scout/position (scout/skip-to-match-start (scout/scanner "testgoal")
                                                      #"yes"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (scout/skip-to-match-start (scout/scan (scout/scanner "test string") #"test")
                                                #"notinthestring")))))

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