function __VERIFIER_error() {}

var foo = 999;

function hoisting(foo) {
  if (typeof foo !== 'function') {
    __VERIFIER_error();
  }

  function foo() {}

  if (typeof foo !== 'function') {
    __VERIFIER_error();
  }
}

hoisting(888);