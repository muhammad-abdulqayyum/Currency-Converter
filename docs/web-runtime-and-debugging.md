# Universal Currency Converter web architecture and debugging

The Angular frontend runs in the browser. The backend is a TypeScript Cloudflare Worker with a Fetch API handler. Wrangler bundles the Worker and removes TypeScript annotations at build time. Cloudflare's workerd runtime executes the resulting JavaScript. Both parts ship together and share a public origin.

## Follow one conversion

1. Angular calls the same-origin endpoint `GET /api/convert?from=USD&to=EUR&amount=100`.
2. The Worker validates the request and checks its rate cache. On a cache miss, it calls Frankfurter from the server.
3. The Worker validates the quote, calculates the conversion, and returns JSON with the amount, rate source, and date.
4. Angular displays the result.

Both runtimes provide `fetch()`. Browser CORS rules govern which cross-origin responses browser JavaScript can read. The Worker’s server-to-server request operates outside browser CORS enforcement. XSS is a separate issue involving script execution in a page. The Worker keeps caching and validation in one place and gives the frontend a stable API.

## What broke in production

The Angular page and health endpoint loaded, and USD-to-USD returned a result using the built-in rate of 1. Conversions requiring external quotes returned HTTP 503. A direct request to Frankfurter succeeded, which narrowed the investigation to the Worker’s rate lookup.

Cloudflare Observability showed that the deployed runtime rejected `redirect: 'error'` before sending the outbound request. The generic 503 came from our error handler. Local tests used mocked fetch responses, so they missed the production runtime incompatibility.

We switched to `redirect: 'manual'` and kept the existing `response.ok` check. With `follow`, fetch would automatically request the redirect destination. With `manual`, the Worker can reject the 3xx response and keep its request tied to the configured provider URL. After deployment, we verified successful USD conversions to EUR, gold, and Bitcoin.

[Production fix](https://github.com/muhammad-abdulqayyum/Currency-Converter/pull/9)

## What to carry forward

A type check verifies what the declared types accept. A runtime check verifies what the deployed environment implements. A health check shows the service responds. A conversion smoke test exercises the provider path as well. For future releases, test a representative fiat and crypto conversion after deployment and check the Worker logs if either fails.

## Interview version

“I built an Angular frontend and a TypeScript API on Cloudflare Workers. The app loaded, but conversions that needed external rates returned 503. I compared a built-in USD conversion with a provider-backed conversion and checked Frankfurter directly. Cloudflare logs identified an unsupported fetch redirect option. I changed it to manual redirect handling, kept the response status validation, and verified fiat, gold, and Bitcoin conversions in production.”
