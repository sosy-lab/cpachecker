function __VERIFIER_error() {}

function immediatelyReturn(value) {
  return (function () {
    return value;
  })();
}

if (immediatelyReturn(1) !== 1) { __VERIFIER_error(); }
if (immediatelyReturn(2) !== 2) { __VERIFIER_error(); }
