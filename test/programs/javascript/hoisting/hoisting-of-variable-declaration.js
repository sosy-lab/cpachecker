function __VERIFIER_error() {}

var foo = 999;

function hoisting() {
  if (foo !== undefined) {
    __VERIFIER_error();
  }

  var foo = 42;

  if (foo !== 42) {
    __VERIFIER_error();
  }
}

hoisting();