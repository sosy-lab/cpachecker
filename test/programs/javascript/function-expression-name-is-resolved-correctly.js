function __VERIFIER_error() {}

var func = function f() {
  return f;
};

if (func !== func()) {
  __VERIFIER_error();
}
