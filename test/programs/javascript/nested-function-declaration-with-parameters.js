function __VERIFIER_error() {}

// verify return values of nested function declarations
function foobar(f) {
  if (f !== 7) { __VERIFIER_error(); }

  function bar(b) {
    if (b !== 3) { __VERIFIER_error(); }
    return 2 + b;
  }

  if (bar(3) !== 5) { __VERIFIER_error(); }
  return 1 + f;
}

if (foobar(7) !== 8) { __VERIFIER_error(); }
