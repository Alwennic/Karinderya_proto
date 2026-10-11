#!/bin/sh
# Karinderya_proto smoke test.
# Run from the repo root after "docker compose up -d".
# It calls the proxy container itself, so it also works when Jenkins runs
# inside a container (where localhost:8080 is not the app).
# Exit code 0 = everything answered, 1 = something is down.

TRIES=12
WAIT=5

check() {
  name="$1"
  path="$2"
  if docker compose exec -T proxy wget -qO- "http://localhost$path" > /tmp/smoke_out 2>/dev/null; then
    echo "PASS  $name ($path)"
    return 0
  fi
  echo "FAIL  $name ($path)"
  return 1
}

attempt=1
while [ "$attempt" -le "$TRIES" ]; do
  echo "--- Smoke test attempt $attempt of $TRIES ---"
  failed=0

  check "frontend page"   "/"                  || failed=1
  check "catalog health"  "/api/menu/health"   || failed=1
  check "auth health"     "/api/auth/health"   || failed=1
  check "menu list"       "/api/menu/items"    || failed=1

  if [ "$failed" -eq 0 ]; then
    echo "Smoke test passed"
    exit 0
  fi

  echo "Waiting for services..."
  sleep "$WAIT"
  attempt=$((attempt + 1))
done

echo "Smoke test failed"
exit 1
