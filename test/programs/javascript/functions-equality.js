function __VERIFIER_error() {}

function f() {}
function g() {}
var fRef = f;

if (f !== f) {
  __VERIFIER_error();
}

if (f !== fRef) {
  __VERIFIER_error();
}

if (f === g) {
  __VERIFIER_error();
}
