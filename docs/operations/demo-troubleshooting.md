# Demo deployment troubleshooting

Quick notes for restoring the public demo when Railway services were previously stopped.

## Backend deployment does not start

If Railway fails during initialization before the deploy phase:

- confirm PostgreSQL is online;
- retry the latest backend deployment;
- inspect the initialization logs before changing application code;
- avoid changing Docker or environment configuration unless the failure reaches the application startup stage.

## Demo verification

After a successful redeploy:

- open the public frontend;
- confirm published events are visible;
- test one demo login;
- verify the backend readiness endpoint responds successfully.

These notes are intentionally short and focused on recovering the demo safely.
