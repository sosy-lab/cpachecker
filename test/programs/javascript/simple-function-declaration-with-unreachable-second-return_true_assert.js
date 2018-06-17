function __VERIFIER_error() {}

function foo() {
  return 42;
  return 1;
}

if (foo() !== 42) { __VERIFIER_error(); }
