import type { Asset } from './assets';

/** Number of fractional digits displayed for the destination asset.
 * World currencies follow ISO 4217 currency minor units as exposed by Intl.
 * Metals and five parent crypto assets retain the existing 8-place policy.
 * SAT and DROP are whole-number denominations.
 */
export function displayPlaces(asset: Pick<Asset, 'code' | 'kind'>): number {
  if (asset.kind === 'metal') return 8;
  if (asset.kind === 'crypto') return asset.code === 'SAT' || asset.code === 'DROP' ? 0 : 8;
  try {
    return new Intl.NumberFormat('en', { style: 'currency', currency: asset.code })
      .resolvedOptions().maximumFractionDigits ?? 2;
  } catch {
    return 2; // Defensive fallback for nonstandard codes or missing Intl data.
  }
}

/** Smallest amount expressible with the destination's configured display precision. */
export function minimumDisplayUnit(places: number): string {
  if (!Number.isInteger(places) || places < 0 || places > 24) throw new RangeError('Invalid display precision');
  return places === 0 ? '1' : '0.' + '0'.repeat(places - 1) + '1';
}

// Exact decimal parsing for display comparisons (never convert to a JS number).
// Match the accepted decimal shape of the conversion engine.
function parts(value: string): [bigint, bigint] {
  if (!/^\d{1,24}(\.\d{1,24})?$/.test(value)) throw new Error('Invalid decimal');
  const [whole, fraction = ''] = value.split('.');
  return [BigInt(whole + fraction), 10n ** BigInt(fraction.length)];
}

export interface DisplayConversion {
  amount: string;
  result: string;
  quotes: readonly { rate: string }[];
}

/**
 * Display a strict upper bound for an exact positive conversion below the
 * destination's smallest displayed unit. Compare original rates with bigint,
 * not the already rounded API result. Preserve zero and normal results.
 */
export function displayConversionResult(
  conversion: DisplayConversion,
  destination: Pick<Asset, 'code' | 'kind'>,
): string {
  if (conversion.quotes?.length !== 2) return conversion.result;
  try {
    const [amount, amountScale] = parts(conversion.amount);
    if (amount === 0n) return conversion.result;
    const [fromRate, fromScale] = parts(conversion.quotes[0].rate);
    const [toRate, toScale] = parts(conversion.quotes[1].rate);
    if (fromRate <= 0n || toRate <= 0n) return conversion.result;
    const places = displayPlaces(destination);
    const belowUnit = amount * toRate * fromScale * (10n ** BigInt(places))
      < amountScale * toScale * fromRate;
    return belowUnit ? '< ' + minimumDisplayUnit(places) : conversion.result;
  } catch {
    // An unexpected or malformed response must not crash Angular's template.
    return conversion.result;
  }
}
