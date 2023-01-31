# Available Endpoints

### Tenants Endpoints

- Create Tentant (with or without provisioning resources with Terraform)
```
Request

POST http://localhost:8082/create-tenant
Content-Type: application/json
{
	"name": "silver"
  "withResources": true
}


Response
{
	"displayName": "silver",
	"passwordSignInAllowed": true,
	"emailLinkSignInEnabled": true,
	"tenantId": "<value>"
}
```

- List Tenants
```
Request

GET http://localhost:8082/list-tenants
Content-Type: application/json

Response
[
  {
    "displayName": "silver",
    "passwordSignInAllowed": true,
    "emailLinkSignInEnabled": true,
    "tenantId": "<value>"
  }
]
```

- Delete Tentant
```
Request

DELETE http://localhost:8082/delete-tenant
```

### Users Endpoints
- Sign Up User (Create account)
```
Request

POST http://localhost:8082/sign-up
Content-Type: application/json
{
  "email": "test-sign-up@qr-regular.com",
  "password": "user123",
  "tenantId": "regular-92it7"
}


Response
{
	"userId": "<value>",
	"tenantId": "<value>",
	"idToken": "<value>"
}
```
- Login
```
Request

POST http://localhost:8082/login
Content-Type: application/json
{
  "email": "user@qr-regular.com",
  "password": "user123",
  "tenantId": "regular-92it7"
}


Response
{
	"userId": "<value>",
	"tenantId": "<value>",
	"idToken": "<value>"
}
```

- Verify User's token `// internal endpoint which trafic of /secure/ is routed to`
```
Request

POST http://localhost:8082/verify
# Add Custom HTTP Header
Id-Token="<idToken-response-value-from-/login-route>"
```