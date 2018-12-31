function __VERIFIER_error() {}

var foo = null;
foo = function () {
  return 1;
};

if (foo() !== 1) { __VERIFIER_error(); }

foo = function () {
  return 2;
};

if (foo() !== 2) { __VERIFIER_error(); }
