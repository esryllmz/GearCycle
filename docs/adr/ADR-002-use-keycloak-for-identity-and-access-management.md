# ADR-002: Use Keycloak for Identity and Access Management

Status: Proposed

> **Revision note (2026-06-26):** Adds clarifications on JWT signature validation/key rotation behavior during a Keycloak outage, and explicitly defers the realm-roles-vs-client-roles modeling choice rather than deciding it here.

## Context

GearCycle requires authentication and role-based authorization across multiple actors (ADMIN, INTAKE_SPECIALIST, TECHNICIAN, INVENTORY_MANAGER, QUALITY_INSPECTOR, SALES_OPERATOR) and across multiple backend services plus a Next.js frontend. The project's technology direction specifies Spring Security OAuth2 Resource Server on the backend and explicitly calls for Keycloak as the identity and access management component, treated as an external system rather than a GearCycle business service.

Building and maintaining a custom identity provider (user storage, password handling, token issuance, role management) would duplicate a well-solved problem and would not demonstrate any GearCycle-specific domain skill — it would only add risk (security-sensitive code that is easy to get wrong) and maintenance burden without being justified by an actual domain requirement.

## Decision

Use Keycloak as an external OAuth2/OpenID Connect identity provider.

- Keycloak issues access tokens (JWTs) after authenticating users.
- Each backend service is an OAuth2 Resource Server (Spring Security) that validates incoming tokens and enforces authorization using roles/claims present in the token.
- Resource servers validate JWT signatures **locally**, using key material (JWKS) obtained from Keycloak, rather than calling back to Keycloak on every request to validate each token. A practical consequence: a temporary Keycloak outage primarily affects login, token refresh, and key rotation — it does not necessarily invalidate access tokens that were already issued and whose signing key resource servers have already cached, until those tokens expire. This must not be treated as a guarantee of continued operation during an outage (refresh and new logins will still fail), only as an accurate description of resource server behavior so it is not over- or under-stated when reasoning about availability.
- `api-gateway` forwards tokens to downstream services rather than re-authenticating or re-issuing them.
- `gearcycle-web` (Next.js) obtains tokens via the standard OAuth2/OIDC authorization code flow and does not store access or refresh tokens in browser `localStorage`, per the project's coding standards.
- GearCycle services do not store passwords or implement their own login flow. Role assignment (which actor maps to which of the six roles) is managed in Keycloak, not duplicated in a GearCycle user table, unless a service needs to store additional domain-specific actor data — in which case it stores only a reference to the Keycloak subject identifier, not credentials.
- Whether GearCycle's six roles are modeled as Keycloak **realm roles** or **client roles** is left as an explicit implementation decision, to be made when Keycloak realm configuration is first implemented, not decided in this ADR. The guiding principle for that later decision: prefer the smallest role model that avoids duplicated permissions across services — for example, do not define the same role redundantly as both a realm role and a per-client role unless a concrete authorization requirement demands the distinction.

## Alternatives Considered

1. **Custom-built authentication and user management.** Rejected: this would require implementing password storage, token issuance, and role management from scratch — substantial security-sensitive surface area that does not demonstrate any GearCycle domain capability and contradicts the principle of not introducing complexity without a real requirement (here, the requirement points the other way: use an established provider).
2. **Managed cloud identity provider (e.g., a hosted OIDC service).** Viable in a real production deployment, and would remove the operational burden of self-hosting Keycloak. Rejected for this project specifically because the infrastructure direction calls for a self-hosted, Docker-Compose-manageable stack for local development and portfolio demonstration purposes, and because Keycloak is explicitly named in the project's technology direction.
3. **Spring Security's in-memory or JDBC-backed authentication without a dedicated identity provider.** Rejected: this does not demonstrate OAuth2 Resource Server patterns across multiple independent services, which is an explicit goal, and would require reimplementing token issuance and role/claim management manually.

## Consequences

- All backend services depend on Keycloak being available and correctly configured (realm, clients, roles) for authentication to function; this dependency must be modeled in Docker Compose and any future Kubernetes manifests.
- Role-to-permission mapping (see `docs/architecture/device-lifecycle.md` for lifecycle transition permissions) must be kept consistent between Keycloak realm role definitions and each service's authorization checks. Whether that mapping is expressed via realm roles or client roles is not fixed by this ADR; see the Decision section and Revisit Conditions below.
- Local development and testing require a running Keycloak instance (or a Testcontainers-managed one for integration tests), adding setup overhead compared to no-auth or mocked-auth approaches.
- Token validation logic (issuer, audience, signature) must be configured consistently across all resource servers to avoid security misconfiguration.
- Because resource servers validate JWTs locally against cached key material rather than calling Keycloak per request, a brief Keycloak outage does not necessarily cause immediate, system-wide request failures for already-authenticated users; it does block new logins, token refreshes, and key rotation for the duration of the outage. This distinction should inform any future availability/SLA documentation rather than being assumed away or overstated.

## Risks

- Misconfiguration of realm roles or client scopes could lead to either overly permissive or overly restrictive access; this must be covered by controller-level authorization tests per the project's testing standards.
- Running Keycloak locally adds resource and startup-time overhead during development; accepted as a deliberate trade-off for demonstrating a realistic OAuth2 Resource Server setup.
- If actor-role requirements turn out to need attributes Keycloak does not naturally model (e.g., per-device assignment), a hybrid approach (roles in Keycloak, fine-grained domain authorization in each service) will be needed; this is anticipated but not yet designed in detail.
- Deferring the realm-roles-vs-client-roles choice means it could be made inconsistently if different services are implemented by different people at different times without revisiting this guidance. Mitigation: the "smallest role model, no duplicated permissions" principle should be applied uniformly when the choice is actually made, ideally in a single realm-configuration task rather than per-service.

## Revisit Conditions

- Revisit if local Keycloak setup proves too heavy for the project's actual development workflow and a lighter-weight identity approach becomes justified for the portfolio context specifically (as opposed to a production deployment).
- Revisit if a future requirement needs identity federation with an external system that Keycloak cannot reasonably support, which is not anticipated for the MVP.
- Revisit once the realm-roles-vs-client-roles decision is actually made: record that decision and its rationale either as an amendment to this ADR or as a new ADR, rather than leaving it implicit in configuration files.
