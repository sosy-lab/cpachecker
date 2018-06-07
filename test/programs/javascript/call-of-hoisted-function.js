function __VERIFIER_error() {}

function callHoistedNestedFunction() {
  return hoistedNestedFunction();
  function hoistedNestedFunction() {
    return 42;
  }
}

if (callHoistedNestedFunction() !== 42) { __VERIFIER_error(); }
if (hoistedFunction() !== 42) { __VERIFIER_error(); }

function hoistedFunction() {
  return 42;
}