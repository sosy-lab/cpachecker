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
// TODO test other cases of the specification (i.a. multiplication of infinity)
// Remaining cases
if (0 * 3 !== 0) { __VERIFIER_error(); }
if (2 * 3 !== 6) { __VERIFIER_error(); }
if (-2 * 3 !== -6) { __VERIFIER_error(); }
if (-2 * -3 !== 6) { __VERIFIER_error(); }
if (0.2 * 0.5 !== 0.1) { __VERIFIER_error(); }
