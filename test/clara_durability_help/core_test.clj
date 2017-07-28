(ns clara-durability-help.core-test
  (:require [clojure.test :refer :all]
            [clara-durability-help.core :refer :all]
            [clara.rules :refer :all]
            [clara.tools.tracing :as t]
            [clara.rules.durability :as d]
            [clara.rules.durability.fressian :as df]))






(defrule rule-1-foo-is-foo
         [?f1 <- :foo (= ?e (:e this)) (= "foo" (:v this))]
         =>
         (retract! ?f1))


(defrule rule-1
         [?f1 <- :foo (= ?e (:e this)) (= "bar" (:v this))]
         =>
         :default)

(defquery get-all-foos
          []
          [?f1 <- :foo])


(defrecord LocalMemorySerializer [holder]
  d/IWorkingMemorySerializer
  (serialize-facts [_ fact-seq]
    (reset! holder fact-seq))
  (deserialize-facts [_]
    @holder))

(deftest test-without-durability
  (testing "Test inserting of additional fact fires correctly"
    (let [fired (-> (mk-session 'clara-durability-help.core-test :fact-type-fn :a)
                    (insert {:e 123 :a :foo :v "foo" :t 1})
                    fire-rules)

          next-session (-> fired
                           (insert {:e 123 :a :foo :v "bar" :t 2})
                           fire-rules)

          results (query next-session get-all-foos)]

      (is (and (= 1 (count results))
               (= "bar" (:v (:?f1 (first results)))))))))



(deftest test-durability
  (testing "Test inserting of additional fact fires correctly with clara durability"
    (let [fired (-> (mk-session 'clara-durability-help.core-test :fact-type-fn :a)
                    (insert {:e 123 :a :foo :v "foo" :t 1})
                    fire-rules)

          create-serializer (fn [stream] (df/create-session-serializer stream))

          rulebase-baos (java.io.ByteArrayOutputStream.)

          rulebase-serializer (create-serializer rulebase-baos)

          session-baos (java.io.ByteArrayOutputStream.)
          session-serializer (create-serializer session-baos)

          holder (atom [])
          mem-serializer (->LocalMemorySerializer holder)

          _ (d/serialize-rulebase fired
                                  rulebase-serializer)

          _ (d/serialize-session-state fired
                                       session-serializer
                                       mem-serializer)

          rulebase-data (.toByteArray rulebase-baos)
          session-data (.toByteArray session-baos)

          rulebase-bais (java.io.ByteArrayInputStream. rulebase-data)
          session-bais (java.io.ByteArrayInputStream. session-data)

          rulebase-serializer-1 (create-serializer rulebase-bais)
          session-serializer-1 (create-serializer session-bais)

          restored-rulebase (d/deserialize-rulebase rulebase-serializer-1)
          restored (d/deserialize-session-state session-serializer-1
                                                mem-serializer
                                                {:base-rulebase restored-rulebase})
          ;;add new facts to the resorted session
          next-session (-> restored
                           (insert {:e 123 :a :foo :v "bar" :t 2})
                           fire-rules)

          results (query next-session get-all-foos)]


      (is (and (= 1 (count results))
               (= "bar" (:v (:?f1 (first results)))))))))










