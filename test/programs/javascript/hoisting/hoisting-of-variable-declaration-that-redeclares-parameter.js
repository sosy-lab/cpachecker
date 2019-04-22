function __VERIFIER_error() {}

var foo = 999;

function hoisting(foo) {
  if (foo !== 888) {
    __VERIFIER_error();
  }

  var foo = 42;

  if (foo !== 42) {
    __VERIFIER_error();
  }
}

hoisting(888);