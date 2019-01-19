function __VERIFIER_error() {}

var obj = new function Foo() {
  this.prop = 1;
};

if (obj.prop === 1) {
  __VERIFIER_error();
}
