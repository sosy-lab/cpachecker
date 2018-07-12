function __VERIFIER_error() {}

var foo = null;
foo = function () {
  return 1;
};

foo = function () {
  __VERIFIER_error();
};

foo();
