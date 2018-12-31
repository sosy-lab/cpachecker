function __VERIFIER_error() {}

var a = {};
var b = {};
var propertyName = 'b';
// a[propertyName] = b;
a['b'] = b;
if (a[propertyName] !== b) {
  __VERIFIER_error();
}
