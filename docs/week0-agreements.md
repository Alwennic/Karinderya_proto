\# Week 0 Agreements



\- Ports: the app is on 8080, Jenkins is on 8081.

\- Frontend calls GET /api/menu/items, POST and PUT /api/menu/items, POST /api/auth/login.

\- Login returns {"token": "..."}; menu items look like {id, name, category, price, available}.

\- Variable names in .env: DB\_ROOT\_PASSWORD, DB\_USER, DB\_PASSWORD, JWT\_SECRET, ADMIN\_USER, ADMIN\_PASSWORD.

