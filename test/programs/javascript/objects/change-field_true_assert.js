function __VERIFIER_error() {}

var a = {};
var b = {};
var c = {};
a.b = b;
a.b = c;
if (a.b !== c) {
  __VERIFIER_error();
}
