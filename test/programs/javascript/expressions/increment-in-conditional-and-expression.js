function __VERIFIER_error() {}

var a = 0;
// `a` has to be evaluated to `0`.
// As a consequence, `++a` is never evaluated.
var b = a && (++a);
if (a !== 0) { __VERIFIER_error(); }
if (b !== 0) { __VERIFIER_error(); }

var c = 1;
// `c` has to be evaluated to `1`.
// As a consequence, `++c` is evaluated and the new value `0` of `c` is the result of the
// whole expression.
var d = c && (--c);
if (c !== 0) { __VERIFIER_error(); }
if (d !== 0) { __VERIFIER_error(); }
