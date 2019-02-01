function __VERIFIER_error() {}

// TODO use global object definition of NaN and Number.isNaN when it is implemented
var NaN = -(-undefined);
function isNaN(value) {
  return value !== value;
}
if (!isNaN(NaN)) { __VERIFIER_error(); }
if (isNaN(0)) { __VERIFIER_error(); }
