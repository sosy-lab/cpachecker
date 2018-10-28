function __VERIFIER_error() {}

function incrementSideEffectInParameters(p0, p1, p2) {
  if (p0 !== 0) { __VERIFIER_error(); }
  if (p1 !== 1) { __VERIFIER_error(); }
  if (p2 !== 2) { __VERIFIER_error(); }
}
var x = 0;
function increment() {
  ++x;
  return x;
}
incrementSideEffectInParameters(x, ++x, increment());
