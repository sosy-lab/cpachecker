function __VERIFIER_error() {}

function Foo() {}

var f = new Foo;

if (f.__proto__ === Foo.prototype) {
  __VERIFIER_error();
}
