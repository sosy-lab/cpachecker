function __VERIFIER_error() {}

var Foo = function () {};

var f = new Foo();

if (f.__proto__ !== Foo.prototype) {
  __VERIFIER_error();
}
