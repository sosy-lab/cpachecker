function __VERIFIER_error() {}

function Foo() {}

var p = {};
Foo.prototype = p;
var f = new Foo();

if (f.__proto__ === p) {
  __VERIFIER_error();
}
