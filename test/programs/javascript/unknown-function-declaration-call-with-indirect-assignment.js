function __VERIFIER_error() {}

var foo = null;
foo = function () {
  return 1;
};

var bar = foo;

foo = function () {
  __VERIFIER_error();
};

foo = bar;

foo();
