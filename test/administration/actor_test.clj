(ns administration.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [administration.actor :as actor]
            [administration.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-requester! st {:requester-id "requester-1" :name "Alice Administrator" :agency "Health"})
    (store/register-regulation! st {:regulation-id "reg-2024-042" :title "Healthcare Standards Act" :status :pending})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:requester-id "requester-1" :op :draft-briefing :stake :low}
        result (actor/run-request! graph request {:topic :neutral} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "requester-1"))))))

(deftest holds-on-unregistered-requester-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:requester-id "no-such-requester" :op :draft-briefing :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-requester")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; flagging compliance conflict always escalates (governor invariant)
        request {:requester-id "requester-1" :op :flag-compliance-conflict :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "requester-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "requester-1")))))))

(deftest interrupts-on-sensitive-topic
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:requester-id "requester-1" :op :draft-correspondence :stake :medium}
        interrupted (actor/run-request! graph request {:topic :policy-position} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "requester-1")))))
