function __VERIFIER_error() {}

var a = {};
var b = {};
var c = {};
var d = {};
var propD = 'd';
var e = {};
var propE = 'e';
a['b'] = b;
if (a.b !== b) {
  __VERIFIER_error();
}
a.c = c;
if (a['c'] !== c) {
  __VERIFIER_error();
}
a[propD] = d;
if (a.d !== d) {
  __VERIFIER_error();
}
a.e = e;
if (a[propE] !== e) {
  __VERIFIER_error();
}
