function __VERIFIER_error() {}

// unary plus and minus operator
if (+3 !== 3) { __VERIFIER_error(); }
if (3 === -3) { __VERIFIER_error(); }
if (3.0 === -3.0) { __VERIFIER_error(); }
if (+0.0 !== -0.0) { __VERIFIER_error(); }
// boolean is converted to number
if (+true !== 1) { __VERIFIER_error(); }
if (-true !== -1) { __VERIFIER_error(); }
if (+false !== 0) { __VERIFIER_error(); }
if (-false !== -0) { __VERIFIER_error(); }
// null is converted to number
if (+null !== 0) { __VERIFIER_error(); }
if (-null !== -0) { __VERIFIER_error(); }
// A reliable way for ECMAScript code to test if a value X is a NaN is an expression of the form
// X !== X. The result will be true if and only if X is a NaN.
var notANumberPositive = +undefined; // undefined is converted to NaN
if (notANumberPositive === notANumberPositive) { __VERIFIER_error(); }
var notANumberNegative = -undefined; // undefined is converted to NaN
if (notANumberNegative === notANumberNegative) { __VERIFIER_error(); }

// void operator
if (void 0 !== undefined) { __VERIFIER_error(); }

// unary not operator
if (!false !== true) { __VERIFIER_error(); }
if (!true !== false) { __VERIFIER_error(); }
if (!0 !== true) { __VERIFIER_error(); }
if (!1 !== false) { __VERIFIER_error(); }
if (!2 !== false) { __VERIFIER_error(); }
if (!notANumberPositive !== true) { __VERIFIER_error(); }
if (!null !== true) { __VERIFIER_error(); }
if (!undefined !== true) { __VERIFIER_error(); }
