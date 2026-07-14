# cloud-itonami-isco-1112

Open Occupation Blueprint for **ISCO-08 1112**: Senior Government Officials.

This repository designs a staff-support actor for a senior government administrator's office: a policy advisor supports an administrator's office administrative workflow (briefing preparation, policy compliance review, inter-agency correspondence, regulation draft summarization) under a governor-gated actor, **while explicitly excluding any binding administrative authority** (policy directives, inter-agency orders, regulatory decisions).

## Scope & Constraints

**This actor supports an administrator's OFFICE, not an administrator as a decision-making agent.** It is a staff-support tool, never a replacement for human administrative judgment or authority. The actor explicitly cannot:

- issue binding policy directives or administrative orders
- represent itself as speaking for the administrator (proposals only)
- claim or exercise any binding administrative power
- disclose civil servant or inter-agency data without verification and human review
- propose policy positions or regulatory stances without explicit human review

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the physical domain work**. Here a policy advisor supports an administrator's office administrative workflow under an actor that proposes actions and an independent **Administrative Governor** that gates them. The governor never dispatches actions without human gating; escalation-category proposals (sensitive inter-agency matters, compliance conflicts) require explicit human sign-off.

## Core Contract

```text
policy inquiry + regulation records + office context
        |
        v
Administrative Advisor -> Administrative Governor -> draft briefing/reply, or escalate for review
        |
        v
office actions (gated) + operating records + audit ledger
```

No automated advice can dispatch an office action the governor refuses, suppress an operating record, or disclose regulatory/inter-agency data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) (ISCO-08 `1112`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and [`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors section): a real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph) `StateGraph`, with the Advisor and Governor as distinct graph nodes and human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/administration/store.cljc` — `Store` protocol + `MemStore`: policy records, regulations, office requests, an append-only audit ledger.
- `src/administration/advisor.cljc` — `Advisor` protocol; `mock-advisor` (deterministic, default) proposes an office operation from a request; `llm-advisor` wraps a `langchain.model/ChatModel` — either way the advisor only ever produces a `:propose`-effect proposal, never a committed record, and LLM parse failures always yield `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/administration/governor.cljc` — `AdministrativeGovernor/check`: a pure function, wired as its own `:govern` node. Hard invariants (unregistered requester, unverified regulation, a proposal whose `:effect` isn't `:propose`) always route to `:hold`. Escalation invariants (sensitive topics, `:flag-compliance-conflict`, or low advisor confidence) always route to `:request-approval` — an `interrupt-before` node that the graph checkpoints and only resumes on explicit human approval (`actor/approve!`).
- `src/administration/actor.cljc` — `build-graph`, `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring itself.

Proposal ops (all `:effect :propose` only, closed allowlist):
- `:draft-briefing` — prepare a policy briefing document for administrator review.
- `:draft-correspondence` — prepare a reply to inter-agency correspondence.
- `:summarize-regulation` — prepare a neutral summary of pending regulations.
- `:review-compliance` — review policy compliance against regulation.
- `:flag-compliance-conflict` — surface a potential compliance conflict for the administrator's attention (always escalates).

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
