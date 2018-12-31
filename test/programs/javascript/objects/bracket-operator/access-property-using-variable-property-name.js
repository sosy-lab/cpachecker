function __VERIFIER_error() {}

var a = {};
var b = {};
var propertyName = 'b';
a[propertyName] = b;
if (a[propertyName] !== b) {
  __VERIFIER_error();
}
