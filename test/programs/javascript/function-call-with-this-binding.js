function __VERIFIER_error() {}

var a = {
  p: 42,
  f: function af() {
    return this;
  },
  getP: function getP() {
    return this.p;
  }
};
if (a.f() !== a) {
  __VERIFIER_error();
}
if (a.getP() !== 42) {
  __VERIFIER_error();
}
a.p = 100;
if (a.getP() !== 100) {
  __VERIFIER_error();
}
