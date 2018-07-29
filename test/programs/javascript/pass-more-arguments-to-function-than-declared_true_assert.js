function __VERIFIER_error() {}

function f2(a, b) {
  if (a !== 1) {
    __VERIFIER_error();
  }
  if (b !== 2) {
    __VERIFIER_error();
  }
}

f2(1, 2, 3);

var f2Alias = f2;
f2Alias(1, 2, 3);