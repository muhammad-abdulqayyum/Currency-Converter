# Angular + TypeScript web application

The web app is additive: Android and iOS remain in their existing directories.

- `web/`: Angular standalone frontend, with currency/metals/crypto search and swap.
- `worker/`: same-origin `/api/assets`, `/api/convert`, `/api/health` endpoints.
- `shared/`: asset catalogue and decimal arithmetic used by the API.
- `wrangler.jsonc`: deploys both the compiled Angular app and Worker.

## Cloudflare Workers Git integration

Select the existing Currency-Converter repository, use the repository root, and select the feature branch for initial preview:

- Branch: `feature/cloudflare-web`
- Build command: `npm ci && npm --prefix web ci && npm run build`
- Deploy command: `npx wrangler deploy`
- No additional API keys or database bindings are needed for this version.
- Use Workers, not a static-only Pages deployment: the conversion API requires the Worker.

After review, merge the web feature into develop. Promote to master through the normal release process and update the deployment branch accordingly.

## Local development

Use Node 24 or newer:

```
npm ci
npm --prefix web ci
npm run build
npm run dev
```

The Worker development server serves both the compiled frontend and API. Rebuild the Angular frontend after changing its source. `npm test` covers decimal precision and API validation/failure behavior. `web/node_modules/.bin/tsc -p worker/tsconfig.json` checks the backend. `npx wrangler deploy --dry-run` verifies packaging.

## Rates and units

Quotes are USD-based official-source reference values from Frankfurter, following the mobile app's provider choice. Crypto uses ESTV reference data, not live market ticks. Quotes older than seven days or missing required source metadata are rejected. The API returns each quote's effective date, source and retrieval timestamp. Metal units are troy ounces. SAT and DROP are derived from BTC and XRP, respectively. The edge cache reuses validated snapshots for 15 minutes. Amounts and history are not stored.

Rate-provider availability and asset coverage vary. Provider failures return an explicit error rather than fabricated rates. Amount validation caps decimal lengths; arithmetic uses integer coefficients and half-up output rounding. Fiat output uses 2 decimals by default, with selected zero/three decimal currencies; crypto and metals use 8, and SAT/DROP use whole units. Reference values are not executable trading prices.

No live deployment is confirmed until Cloudflare reports deployment success. The initial implementation was built and tested locally; account authentication is required for publication.
