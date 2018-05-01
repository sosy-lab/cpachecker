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
if (+null !== 0) { __VERIFIER_error(); }
if (-null !== -0) { __VERIFIER_error(); }
