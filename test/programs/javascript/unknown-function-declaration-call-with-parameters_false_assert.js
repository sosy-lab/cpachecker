function __VERIFIER_error() {}

var foo = null;
foo = function (a, b) {
  return a + b;
};

foo = function (a, b) {
  if (a < b) {
    __VERIFIER_error();
  }
};

foo(1, 2);
