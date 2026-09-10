(ns administration.store
  "SSoT for the ISCO-08 1112 senior administrative office support actor. Store is a
  protocol injected into the `administration.actor` StateGraph — `MemStore`
  is the default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  Domain:

    requester    — a registered civil servant or inter-agency stakeholder
                   (:requester-id, :name, :agency)
    regulation   — a regulation under review (:regulation-id, :title, :status)
    record       — a committed office record (briefing drafted, inter-agency
                   correspondence drafted, regulation summarized, compliance
                   reviewed, conflict flagged) — written ONLY via
                   commit-record!, never mutated in place
    ledger       — an append-only audit trail of every proposal/verdict/
                   disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (requester [s requester-id])
  (regulation [s regulation-id])
  (records-of [s requester-id])
  (ledger [s])
  (register-requester! [s requester])
  (register-regulation! [s regulation])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (requester [_ requester-id] (get-in @a [:requesters requester-id]))
  (regulation [_ regulation-id] (get-in @a [:regulations regulation-id]))
  (records-of [_ requester-id] (filter #(= requester-id (:requester-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-requester! [s requester]
    (swap! a assoc-in [:requesters (:requester-id requester)] requester) s)
  (register-regulation! [s regulation]
    (swap! a assoc-in [:regulations (:regulation-id regulation)] regulation) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:requesters {} :regulations {} :records [] :ledger []} seed)))))
