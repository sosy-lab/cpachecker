function __VERIFIER_error() {}

function foo() {
  var x = 42
}

// verify default return value
if (foo() !== undefined) { __VERIFIER_error(); }
