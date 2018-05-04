function __VERIFIER_error() {}

// verify return values of nested function declarations
function foobar() {
  function bar() {
    return 1;
  }

  if (bar() !== 1) { __VERIFIER_error(); }
  return 0;
}

if (foobar() !== 0) { __VERIFIER_error(); }
