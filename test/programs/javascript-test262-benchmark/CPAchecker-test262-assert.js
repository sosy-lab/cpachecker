function __VERIFIER_error() {}

function $ERROR(message) {
  __VERIFIER_error();
}

var assert = {
  sameValue: function sameValue(actual, expected) {
    if (actual !== expected) {
      __VERIFIER_error();
    }
  }
};
