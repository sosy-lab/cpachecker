function __VERIFIER_error() {}

var x = 0;
var obj = {
  foo: x,
  bar: {
    foobar: ++x
  },
  foobar: ++x
};
if (obj.foo !== 0) { __VERIFIER_error(); }
if (obj.bar.foobar !== 1) { __VERIFIER_error(); }
if (obj.foobar !== 2) { __VERIFIER_error(); }
