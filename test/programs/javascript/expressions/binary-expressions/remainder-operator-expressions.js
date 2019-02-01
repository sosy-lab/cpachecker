function __VERIFIER_error() {}

// TODO use global object definition of NaN and Number.isNaN when it is implemented
var NaN = -(-undefined);
function isNaN(value) {
  return value !== value;
}

// -------------------------------------------------------------------------------
// remainder, see https://www.ecma-international.org/ecma-262/5.1/#sec-11.5.3
// -------------------------------------------------------------------------------
// If either operand is NaN, the result is NaN.
if (!isNaN(NaN % 3)) { __VERIFIER_error(); }
if (!isNaN(3 % NaN)) { __VERIFIER_error(); }
// The sign of the result equals the sign of the dividend.
// TODO remainder of floating point numbers is not implemented yet
// if ( 6 %  4 !==  2) { __VERIFIER_error(); }
// if (-6 % -4 !== -2) { __VERIFIER_error(); }
// if ( 6 % -4 !==  2) { __VERIFIER_error(); }
// if (-6 %  4 !== -2) { __VERIFIER_error(); }
// // If the dividend is an infinity, or the divisor is a zero, or both, the result is NaN.
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
// TODO remainder of floating point numbers is not implemented yet
// if ( 3.2 %  2 !==  1.2000000000000002) { __VERIFIER_error(); }
// if ( 3.2 % -2 !==  1.2000000000000002) { __VERIFIER_error(); }
// if (-3.2 %  2 !== -1.2000000000000002) { __VERIFIER_error(); }
// if (-3.2 % -2 !== -1.2000000000000002) { __VERIFIER_error(); }
