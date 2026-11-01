# API guide

Open `/swagger-ui.html` after logging into the web application. `/v3/api-docs` serves generated OpenAPI; it is permission-authenticated. JSON errors have `message`. The response does not leak stack traces. Request bodies are map-shaped for this portfolio API; examples here supply the intended contracts.

## Authentication

GET `/api/csrf` returns `{token,headerName}` and a session cookie. POST `/api/login` uses URL-encoded username/password plus that CSRF header and cookie. On success fetch `/api/csrf` again and retain the rotated session. GET `/api/me` gives server-derived roles/permissions. POST `/api/logout` invalidates the session. No browser-supplied role flags are accepted.

## Endpoints

| Area | Routes |
| --- | --- |
| Workspace | GET `/api/dashboard`, `/api/property`, `/api/demo`, `/api/me` |
| Patrons | GET/POST `/api/patrons`; GET/PUT `/api/patrons/{id}`; POST `/api/patrons/{id}/merge`; POST `/api/patrons/{id}/w9` |
| Transactions | GET/POST `/api/transactions`; POST `/api/transactions/{id}/post`, `/correct`, `/void`, `/delete` |
| Definitions | GET/POST `/api/types`; PUT `/api/types/{id}`; POST `/api/types/{id}/activate`, `/deactivate` |
| Reports | GET `/api/reports?kind=CTR|SAR|TAX`; POST `/api/reports`; GET/PUT `/api/reports/{id}` |
| Workflow | POST `/api/reports/{id}/review`, `/approve`, `/return`, `/reject`, `/close`, `/correction` |
| Review evidence | GET `/api/reports/{id}/validation`, `/versions`, `/transactions`, `/subjects`; POST `/subjects` |
| Batches | GET `/api/batches?kind=...`; POST `/api/batches`; GET `/api/batches/{id}/items`; POST `/export`, `/submit`, `/reconcile` |
| Attachments | GET/POST `/api/attachments?ownerType=Report|Patron|Transaction&ownerId=...`; GET `/api/attachments/{id}/download` |
| Governance | GET `/api/users`, `/api/configurations`; POST `/api/users`; PUT `/api/configurations/{id}`; PUT `/api/property` |
| Audit / tasks | GET `/api/audit?entityType=...&entityId=...`; POST `/api/tasks/{id}/resolve` |
| Operations | GET `/actuator/health` (unauthenticated, no DB credentials/details), `/v3/api-docs` (authenticated) |

Collections are loaded into the UI and paginated/searched there. This is not a server-paginated 100,000-patron search implementation; see measured-target limitations. Exports and report evidence are still permission checked by the server.

## Examples

Create synthetic patron:

```json
{"legalName":"Demo Example","dateOfBirth":"1985-01-01","address":"100 Fictional Way, Demo City","country":"US","identityStatus":"VERIFIED","identityNumber":"DEMO-ID-9001","tin":"DEMO-TIN-9001","identityExpiry":"2030-01-01","residency":"RESIDENT","reason":"Create synthetic demonstration patron"}
```

Post cash-in (look up real seeded type/patron IDs first):

```json
{"sourceId":"DEMO-IMPORT-0001","patronId":1,"typeId":1,"gross":"10001.00","cash":"10001.00","occurredAt":"2026-10-31T12:00:00-04:00","reference":"DEMO-TABLE-01","station":"Main cage","status":"POSTED","reason":"Synthetic cage posting"}
```

A source ID replay returns the existing transaction if its identifying payload agrees; a conflicting amount/patron/type/time returns an error. Voids/corrections require optimistic `version` and a reason. Source backdating uses the recorded gaming boundary.

Send a report for review (approve uses the same shape but an independent authorized identity):

```json
{"version":0,"reason":"Reviewed synthetic source activity and identity"}
```

Create SAR:

```json
{"kind":"SAR","patronId":2,"detectionDate":"2026-10-31","activityStart":"2026-10-30","activityEnd":"2026-10-31","amount":"7500.00","category":"POSSIBLE_STRUCTURING","narrative":"SYNTHETIC TRAINING: repeated example activity is referred for independent review. No real person or finding is represented.","reason":"Synthetic analyst investigation"}
```

Batch creation:

```json
{"kind":"CTR","reportIds":[3,4]}
```

Submit simulation:

```json
{"version":0,"reason":"Synthetic transport workflow test","scenario":"PARTIAL"}
```

Always obtain fresh version numbers from the API. HTTP 409 signals uniqueness/concurrent changes. Validation uses 400; unauthorized list access uses 403; hidden individual SAR records use 404. Multipart evidence upload requires file, ownerType, ownerId and reason. Download is always as a file attachment, never inline HTML.
