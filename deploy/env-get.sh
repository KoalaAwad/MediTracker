# Sourced by the deploy scripts. Reads one key from deploy/.env.
# .env uses compose syntax (unquoted values with spaces or <>), so it isn't safe to `source`.
env_get() {
  [ -f .env ] || { echo "deploy/.env is missing" >&2; return 1; }
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -E 's/^"(.*)"$/\1/; s/^'"'"'(.*)'"'"'$/\1/' || true
}
