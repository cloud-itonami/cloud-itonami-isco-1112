# Security Policy

## Reporting Security Issues

**Do not** open public GitHub issues for security vulnerabilities.

If you discover a security vulnerability, please email the maintainers directly with:
- A description of the vulnerability
- Steps to reproduce it
- Potential impact

We will work with you to understand and address the issue promptly.

## Security Considerations

This is a reference implementation of an ISCO-08 1112 administrative office support actor. Like all safety-critical systems, it should be reviewed thoroughly before use in production contexts.

Key security properties this implementation enforces:

- **No unauthenticated action**: The AdministrativeGovernor requires registration verification before any proposal can proceed.
- **Audit trail**: Every proposal, verdict, and decision is logged in an append-only ledger.
- **Human-in-the-loop escalation**: Sensitive decisions are checkpointed and require explicit human approval before resuming.
- **No direct actuation**: The actor can only propose; it cannot itself commit, dispatch, or enforce actions.
