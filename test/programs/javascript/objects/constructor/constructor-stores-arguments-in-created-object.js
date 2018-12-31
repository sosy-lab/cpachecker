function __VERIFIER_error() {}

function Foo(prop) {
  this.prop = prop;
}

Foo.prototype.getProp = function () {
  return this.prop;
};

var f = new Foo(42);

if (f.prop !== 42) {
  __VERIFIER_error();
}

if (f.getProp() !== 42) {
  __VERIFIER_error();
}
