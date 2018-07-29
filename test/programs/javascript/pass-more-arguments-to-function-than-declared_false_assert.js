function __VERIFIER_error() {}

function f2(a, b) {
  if (a === 1) {
    if (b === 2) {
      __VERIFIER_error();
    }
  }
}

var f2Alias = f2;
f2Alias(1, 2, 3);