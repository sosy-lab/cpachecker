function __VERIFIER_error() {}

// TODO use global object definition of NaN and Number.isNaN when it is implemented
var NaN = -(-undefined);
function isNaN(value) {
  return value !== value;
}
if (!isNaN(NaN)) { __VERIFIER_error(); }
if (isNaN(0)) { __VERIFIER_error(); }

// strict (un-) equality
if (2 === 3) { __VERIFIER_error(); }
if (3 !== 3) { __VERIFIER_error(); }
if (3 !== 3.0) { __VERIFIER_error(); }
if (3 !== 3.0) { __VERIFIER_error(); }

// binary operators
if (1 + 2 !== 3) { __VERIFIER_error(); }
if (3 + 2 === 3) { __VERIFIER_error(); }
if (5 - 2 !== 3) { __VERIFIER_error(); }
if (5 - 3 === 3) { __VERIFIER_error(); }

// -------------------------------------------------------------------------------
// multiplication, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.5.1
// -------------------------------------------------------------------------------
// If either operand is NaN, the result is NaN.
if (!isNaN(NaN * 3)) { __VERIFIER_error(); }
if (!isNaN(3 * NaN)) { __VERIFIER_error(); }
// The sign of the result is positive if both operands have the same sign,
// negative if the operands have different signs.
if ( 2 *  3 !==  6) { __VERIFIER_error(); }
if (-2 * -3 !==  6) { __VERIFIER_error(); }
if ( 2 * -3 !== -6) { __VERIFIER_error(); }
if (-2 *  3 !== -6) { __VERIFIER_error(); }
// Multiplication of an infinity by a zero results in NaN.
if (!isNaN( Infinity * 0)) { __VERIFIER_error(); }
if (!isNaN(-Infinity * 0)) { __VERIFIER_error(); }
if (!isNaN(0 *  Infinity)) { __VERIFIER_error(); }
if (!isNaN(0 * -Infinity)) { __VERIFIER_error(); }
// Multiplication of an infinity by an infinity results in an infinity.
// The sign is determined by the rule already stated above.
if ( Infinity *  Infinity !==  Infinity) { __VERIFIER_error(); }
if (-Infinity * -Infinity !==  Infinity) { __VERIFIER_error(); }
if (-Infinity *  Infinity !== -Infinity) { __VERIFIER_error(); }
if ( Infinity * -Infinity !== -Infinity) { __VERIFIER_error(); }
// Multiplication of an infinity by a finite nonzero value results in a signed infinity.
// The sign is determined by the rule already stated above.
if ( Infinity *  9 !==  Infinity) { __VERIFIER_error(); }
if (-Infinity * -9 !==  Infinity) { __VERIFIER_error(); }
if (-Infinity *  9 !== -Infinity) { __VERIFIER_error(); }
if ( Infinity * -9 !== -Infinity) { __VERIFIER_error(); }
if ( 9 *  Infinity !==  Infinity) { __VERIFIER_error(); }
if (-9 * -Infinity !==  Infinity) { __VERIFIER_error(); }
if ( 9 * -Infinity !== -Infinity) { __VERIFIER_error(); }
if (-9 *  Infinity !== -Infinity) { __VERIFIER_error(); }
// In the remaining cases, where neither an infinity or NaN is involved,
// the product is computed and rounded to the nearest representable value using
// IEEE 754 round-to-nearest mode.
// If the magnitude is too large to represent,
// the result is then an infinity of appropriate sign.
// If the magnitude is too small to represent,
// the result is then a zero of appropriate sign.
// The ECMAScript language requires support of gradual underflow as defined by IEEE 754.
// TODO test edge cases described in comment above
if (0 * 3 !== 0) { __VERIFIER_error(); }
if (2 * 3 !== 6) { __VERIFIER_error(); }
if (-2 * 3 !== -6) { __VERIFIER_error(); }
if (-2 * -3 !== 6) { __VERIFIER_error(); }
if (0.2 * 0.5 !== 0.1) { __VERIFIER_error(); }

// -------------------------------------------------------------------------------
// division, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.5.2
// -------------------------------------------------------------------------------
// If either operand is NaN, the result is NaN.
if (!isNaN(NaN / 3)) { __VERIFIER_error(); }
if (!isNaN(3 / NaN)) { __VERIFIER_error(); }
// The sign of the result is positive if both operands have the same sign, negative if the operands
// have different signs.
if ( 6 /  3 !==  2) { __VERIFIER_error(); }
if (-6 / -3 !==  2) { __VERIFIER_error(); }
if ( 6 / -3 !== -2) { __VERIFIER_error(); }
if (-6 /  3 !== -2) { __VERIFIER_error(); }

if (Infinity !== (9999999 / 0)) { __VERIFIER_error(); }
// Division of an infinity by an infinity results in NaN.
if (!isNaN(Infinity / Infinity)) { __VERIFIER_error(); }
if (!isNaN(Infinity / -Infinity)) { __VERIFIER_error(); }
if (!isNaN(-Infinity / Infinity)) { __VERIFIER_error(); }
if (!isNaN(-Infinity / -Infinity)) { __VERIFIER_error(); }
// Division of an infinity by a zero results in an infinity.
// The sign is determined by the rule already stated above.
if (Infinity / 0 !== Infinity) { __VERIFIER_error(); }
if (-Infinity / -0 !== Infinity) { __VERIFIER_error(); }
if (-Infinity / 0 !== -Infinity) { __VERIFIER_error(); }
if (Infinity / -0 !== -Infinity) { __VERIFIER_error(); }
// Division of an infinity by a nonzero finite value results in a signed infinity.
// The sign is determined by the rule already stated above.
if (Infinity / 9 !== Infinity) { __VERIFIER_error(); }
if (-Infinity / -9 !== Infinity) { __VERIFIER_error(); }
if (-Infinity / 9 !== -Infinity) { __VERIFIER_error(); }
if (Infinity / -9 !== -Infinity) { __VERIFIER_error(); }
// Division of a finite value by an infinity results in zero.
// The sign is determined by the rule already stated above.
if (9 / Infinity !== 0) { __VERIFIER_error(); }
if (-9 / -Infinity !== 0) { __VERIFIER_error(); }
if (9 / -Infinity !== -0) { __VERIFIER_error(); }
if (-9 / Infinity !== -0) { __VERIFIER_error(); }
// Division of a zero by a zero results in NaN
if (!isNaN( 0 /  0)) { __VERIFIER_error(); }
if (!isNaN( 0 / -0)) { __VERIFIER_error(); }
if (!isNaN(-0 /  0)) { __VERIFIER_error(); }
if (!isNaN(-0 / -0)) { __VERIFIER_error(); }
// division of zero by any other finite value results in zero, with the sign determined by the
// rule already stated above.
if ( 0 /  9 !==  0) { __VERIFIER_error(); }
if (-0 / -9 !==  0) { __VERIFIER_error(); }
if ( 0 / -9 !== -0) { __VERIFIER_error(); }
if (-0 /  9 !== -0) { __VERIFIER_error(); }
// Division of a nonzero finite value by a zero results in a signed infinity.
// The sign is determined by the rule already stated above.
if (9 / 0 !== Infinity) { __VERIFIER_error(); }
if (-9 / -0 !== Infinity) { __VERIFIER_error(); }
if ( 9 / -0 !== -Infinity) { __VERIFIER_error(); }
if (-9 /  0 !== -Infinity) { __VERIFIER_error(); }

// -------------------------------------------------------------------------------
// remainder, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.5.3
// -------------------------------------------------------------------------------
// If either operand is NaN, the result is NaN.
if (!isNaN(NaN % 3)) { __VERIFIER_error(); }
if (!isNaN(3 % NaN)) { __VERIFIER_error(); }
// The sign of the result equals the sign of the dividend.
if ( 6 %  4 !==  2) { __VERIFIER_error(); }
if (-6 % -4 !== -2) { __VERIFIER_error(); }
if ( 6 % -4 !==  2) { __VERIFIER_error(); }
if (-6 %  4 !== -2) { __VERIFIER_error(); }
// If the dividend is an infinity, or the divisor is a zero, or both, the result is NaN.
if (!isNaN( Infinity % 3)) { __VERIFIER_error(); }
if (!isNaN(-Infinity % 3)) { __VERIFIER_error(); }
if (!isNaN(3 %  0)) { __VERIFIER_error(); }
if (!isNaN(3 % -0)) { __VERIFIER_error(); }
if (!isNaN( Infinity %  0)) { __VERIFIER_error(); }
if (!isNaN( Infinity % -0)) { __VERIFIER_error(); }
if (!isNaN(-Infinity %  0)) { __VERIFIER_error(); }
if (!isNaN(-Infinity % -0)) { __VERIFIER_error(); }
// If the dividend is finite and the divisor is an infinity, the result equals the dividend.
if ( 3 %  Infinity !==  3) { __VERIFIER_error(); }
if ( 3 % -Infinity !==  3) { __VERIFIER_error(); }
if (-3 %  Infinity !== -3) { __VERIFIER_error(); }
if (-3 % -Infinity !== -3) { __VERIFIER_error(); }
// If the dividend is a zero and the divisor is nonzero and finite,
// the result is the same as the dividend.
if ( 0 %  9 !==  0) { __VERIFIER_error(); }
if ( 0 % -9 !==  0) { __VERIFIER_error(); }
if (-0 %  9 !== -0) { __VERIFIER_error(); }
if (-0 % -9 !== -0) { __VERIFIER_error(); }
// In the remaining cases, where neither an infinity, nor a zero, nor NaN is involved,
// the floating-point remainder r from a dividend n and a divisor d is defined by the mathematical
// relation r = n − (d × q) where q is an integer that is negative only if n/d is negative and
// positive only if n/d is positive, and whose magnitude is as large as possible without exceeding
// the magnitude of the true mathematical quotient of n and d.
// r is computed and rounded to the nearest representable value using
// IEEE 754 round-to-nearest mode.
if ( 3.2 %  2 !==  1.2000000000000002) { __VERIFIER_error(); }
if ( 3.2 % -2 !==  1.2000000000000002) { __VERIFIER_error(); }
if (-3.2 %  2 !== -1.2000000000000002) { __VERIFIER_error(); }
if (-3.2 % -2 !== -1.2000000000000002) { __VERIFIER_error(); }

// ---------------------------------------------------------------------------------------
// The Less-than operator, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.8.1
// ---------------------------------------------------------------------------------------
if (5 < 2) { __VERIFIER_error(); }

// ------------------------------------------------------------------------------------------
// The Greater-than operator, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.8.2
// ------------------------------------------------------------------------------------------
if (2 > 5) { __VERIFIER_error(); }
