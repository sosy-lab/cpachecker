function __VERIFIER_error() {}

var foo = 999;

function hoisting() {
  if (typeof foo !== 'function') {
    __VERIFIER_error();
  }

  var foo = 42;

  function foo() {
  }

  if (foo !== 42) {
    __VERIFIER_error();
  }
}

hoisting();