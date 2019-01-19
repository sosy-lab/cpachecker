function __VERIFIER_error() {}

var Foo = function () {};

var p = {};
Foo.prototype = p;
var f = new Foo();

if (f.__proto__ !== p) {
  __VERIFIER_error();
}
