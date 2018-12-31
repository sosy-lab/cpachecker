function __VERIFIER_error() {}

var obj = {
  __proto__: {
    __proto__: {
      prop: 42
    }
  }
};

if (obj.prop !== 42) {
  __VERIFIER_error();
}
