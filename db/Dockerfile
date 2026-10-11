FROM mysql:8.4
# Schema and seed data run automatically on the first start (empty volume only)
COPY init.sql /docker-entrypoint-initdb.d/init.sql
