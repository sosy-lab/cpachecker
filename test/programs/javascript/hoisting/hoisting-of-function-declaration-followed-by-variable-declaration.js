function __VERIFIER_error() {}

var foo = 999;

function hoisting() {
  if (typeof foo !== 'function') {
    __VERIFIER_error();
  }

  function foo() {
  }

  var foo = 42;

  if (foo !== 42) {
    __VERIFIER_error();
  }
}

hoisting();