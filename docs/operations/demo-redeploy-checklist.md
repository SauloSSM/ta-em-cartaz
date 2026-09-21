# Demo redeploy checklist

Use this when bringing the public demo back online after a pause.

1. Start the PostgreSQL service on Railway.
2. Redeploy the backend service.
3. Confirm the readiness endpoint is healthy.
4. Open the Vercel frontend and verify that published events load.
5. Test one demo login before presenting the project.

This keeps the demo recovery process short and repeatable without changing application code.
