// TODO verify this library

function assert(condition, message) {
  if (condition) {
    __VERIFIER_error();
  }
}

function __VERIFIER_isSameValue(a, b) {
  if (a === b) {
    // Handle +/-0 vs. -/+0
    return a !== 0 || 1 / a === 1 / b;
  }

  // Handle NaN vs. NaN
  return a !== a && b !== b;
}

assert.sameValue = function sameValue(actual, expected) {
  if (__VERIFIER_isSameValue(actual, expected)) {
    __VERIFIER_error();
  }
};
