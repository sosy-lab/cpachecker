function __VERIFIER_error() {}

var foo = null;
foo = function (a, b) {
  return a + b;
};

if (foo(2, 4) !== 6) { __VERIFIER_error(); }

foo = function (a, b) {
  return a - b;
};

if (foo(6, 4) !== 2) { __VERIFIER_error(); }
