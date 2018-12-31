function __VERIFIER_error() {}

var a = {};
var a2 = a;
a2.b = 6666;
var a3 = a;
if (a.b !== 6666) {
  __VERIFIER_error();
}
if (a2.b !== 6666) {
  __VERIFIER_error();
}
if (a3.b !== 6666) {
  __VERIFIER_error();
}
