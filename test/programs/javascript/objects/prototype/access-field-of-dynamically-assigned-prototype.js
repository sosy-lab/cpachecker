function __VERIFIER_error() {}

var obj = {};

if (obj.prop !== undefined) {
  __VERIFIER_error();
}

obj.__proto__ = {
  prop: 42
};

if (obj.prop !== 42) {
  __VERIFIER_error();
}
