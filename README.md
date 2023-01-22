### Available Endpoints

POST http://localhost:8082/login
Content-Type: application/json
{
  "email": "user@qr-regular.com"
  "password": "user123"
}

---

`// internal endpoint which trafic of /secure/ is routed to`
GET http://localhost:8082/verify
Header:
`USER_ID_TOKEN="<token>"`
