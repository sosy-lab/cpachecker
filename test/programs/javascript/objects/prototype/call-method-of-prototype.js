function __VERIFIER_error() {}

var obj = {
  prop: 42,
  __proto__: {
    getProp: function () {
      return this.prop;
    }
  }
};

if (obj.getProp() !== 42) {
  __VERIFIER_error();
}
