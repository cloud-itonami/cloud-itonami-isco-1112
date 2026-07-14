(ns administration.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [administration.governor :as governor]
            [administration.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-requester! st {:requester-id "requester-1" :name "Alice Administrator" :agency "Health"})
    (store/register-regulation! st {:regulation-id "reg-2024-042" :title "Healthcare Standards Act" :status :pending})
    st))

(deftest hard-violations-on-unregistered-requester
  (let [st (fresh-store)
        request {:requester-id "no-such-requester" :op :draft-briefing :stake :low}
        proposal {:op :draft-briefing :effect :propose :confidence 0.9 :stake :low}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))
    (is (false? (:escalate? verdict)))
    (is (some? (seq (:violations verdict))))))

(deftest hard-violations-on-unregistered-regulation
  (let [st (fresh-store)
        request {:requester-id "requester-1" :regulation-id "no-such-reg" :op :summarize-regulation}
        proposal {:op :summarize-regulation :effect :propose :confidence 0.9 :stake :medium}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))
    (is (false? (:escalate? verdict)))))

(deftest hard-violation-on-non-propose-effect
  (let [st (fresh-store)
        request {:requester-id "requester-1" :op :draft-briefing}
        proposal {:op :draft-briefing :effect :commit :confidence 0.9}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (true? (:hard? verdict)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        request {:requester-id "requester-1" :op :draft-briefing :stake :low}
        proposal {:op :draft-briefing :effect :propose :confidence 0.4 :stake :low}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest escalates-on-sensitive-topic
  (let [st (fresh-store)
        request {:requester-id "requester-1" :op :draft-correspondence}
        proposal {:op :draft-correspondence :effect :propose :confidence 0.9 :stake :medium}
        context {:topic :policy-position}
        verdict (governor/check request context proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest escalates-on-flag-compliance-conflict
  (let [st (fresh-store)
        request {:requester-id "requester-1" :op :flag-compliance-conflict}
        proposal {:op :flag-compliance-conflict :effect :propose :confidence 0.95}
        verdict (governor/check request {} proposal st)]
    (is (false? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (true? (:escalate? verdict)))))

(deftest accepts-clean-low-risk-request
  (let [st (fresh-store)
        request {:requester-id "requester-1" :op :draft-briefing :stake :low}
        proposal {:op :draft-briefing :effect :propose :confidence 0.95 :stake :low}
        verdict (governor/check request {:topic :neutral} proposal st)]
    (is (true? (:ok? verdict)))
    (is (false? (:hard? verdict)))
    (is (false? (:escalate? verdict)))))
