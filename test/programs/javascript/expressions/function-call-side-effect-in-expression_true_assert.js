function __VERIFIER_error() {}

var x = 0;
function increment() {
  ++x;
  return x;
}
var y = x + increment();
if (y !== 1) { __VERIFIER_error(); }
