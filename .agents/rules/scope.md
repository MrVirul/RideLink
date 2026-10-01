---
description: "Strict boundary rules limiting agent scope to the account-service only."
---

# Microservices Scope Rule

1. **Strict Service Boundaries**: The user is solely responsible for the `account-service`, the `api-gateway`, and the `service-registry`. 
2. **Do Not Touch Other Services**: Under NO circumstances should you read, edit, configure, or run anything inside the following directories:
   - `driver-service/`
   - `ride-service/`
   - `fare-service/`
3. **No Database Requirements for Other Services**: Do not prompt the user to setup databases or environment variables for the excluded services listed above.

Always restrict your context and assistance strictly to the `account-service` unless explicitly told otherwise.
