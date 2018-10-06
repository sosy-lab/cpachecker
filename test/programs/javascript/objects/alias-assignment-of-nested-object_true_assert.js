function __VERIFIER_error() {}

var a = {};
var b = {};
var c = {};
a.b = b;
var b2 = a.b;
b2.c = c;
var b3 = a.b;
if (b.c !== c) {
  __VERIFIER_error();
}
if (b2.c !== c) {
  __VERIFIER_error();
}
if (b3.c !== c) {
  __VERIFIER_error();
}
