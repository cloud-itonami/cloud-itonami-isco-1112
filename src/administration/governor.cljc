(ns administration.governor
  "AdministrativeGovernor — the independent safety/traceability layer for the
  ISCO-08 1112 senior administrative office support actor. Wired as its own `:govern`
  node in `administration.actor`'s StateGraph, downstream of `:advise` — the
  Advisor has no notion of requester provenance, regulation verification, or
  sensitive-topic risk, so this MUST be a separate system able to reject a
  proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. requester provenance    — if requester-id is provided, it must
       be registered.
    2. regulation verification — if regulation-id is provided, it must be
       registered and verified.
    3. no-actuation            — proposal :effect must be :propose.
    4. no-binding-authority    — no :op that claims or exercises binding
       administrative power.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    5. :flag-compliance-conflict always escalates (sensitive by definition).
    6. topic sensitivity       — topics tagged as legally/politically
       sensitive must escalate for administrator review.
    7. low confidence (< `confidence-floor`)."
  (:require [administration.store :as store]))

(def confidence-floor 0.6)
(def ^:private sensitive-topics #{:policy-position :inter-agency-directive :regulatory-decision})
(def ^:private escalating-ops #{:flag-compliance-conflict})

(defn- hard-violations [{:keys [proposal request]} requester-record regulation-record]
  (cond-> []
    (and (some? (:requester-id request))
         (nil? requester-record))
    (conj {:rule :no-requester :detail "unregistered requester"})

    (and (some? (:regulation-id request))
         (nil? regulation-record))
    (conj {:rule :no-regulation :detail "unregistered or unverified regulation"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct dispatch)"})

    (get request :no-binding-authority)
    (conj {:rule :no-binding-authority :detail "actor has no binding administrative power (directives, orders)"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `administration.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [requester-record (when (some? (:requester-id request))
                           (store/requester store (:requester-id request)))
        regulation-record (when (some? (:regulation-id request))
                            (store/regulation store (:regulation-id request)))
        hard (hard-violations {:proposal proposal :request request}
                              requester-record regulation-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        flagging-conflict? (contains? escalating-ops (:op proposal))
        topic-sensitive? (contains? sensitive-topics (:topic context))
        risky? (or flagging-conflict? topic-sensitive?)]
    {:ok? (and (not hard?) (not low?) (not risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky?))}))
