/** Decimal arithmetic with integer coefficients; round half up only at output. */
export function decimal(value: string): [bigint, bigint] {
  if (!/^\d{1,24}(\.\d{1,24})?$/.test(value)) throw new Error('Enter a nonnegative decimal with at most 24 digits on each side.');
  const [whole, fraction = ''] = value.split('.');
  return [BigInt(whole + fraction), 10n ** BigInt(fraction.length)];
}
export function convert(amount: string, from: string, to: string, places = 8): string {
  const [a, ad] = decimal(amount), [f, fd] = decimal(from), [t, td] = decimal(to);
  if (f <= 0n || t <= 0n) throw new Error('Invalid reference rate');
  const scale = 10n ** BigInt(places), n = a * t * fd * scale, d = ad * td * f;
  const rounded = (n * 2n + d) / (2n * d);
  const whole = rounded / scale, fraction = (rounded % scale).toString().padStart(places, '0').replace(/0+$/, '');
  return whole.toString() + (fraction ? '.' + fraction : '');
}
