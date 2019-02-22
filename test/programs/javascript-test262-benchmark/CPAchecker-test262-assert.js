// TODO verify this library

function assert(condition, message) {
  if (!condition) {
    __VERIFIER_error();
  }
}

assert.sameValue = function sameValue(actual, expected) {
  if (actual !== expected) {
    __VERIFIER_error();
  }
};
