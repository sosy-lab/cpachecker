function __VERIFIER_error() {}

var obj = {
  foo: 6666,
  bar: {
    foobar: 7777
  }
};
if (obj.foo !== 6666) { __VERIFIER_error(); }
if (obj.bar.foobar !== 7777) { __VERIFIER_error(); }
if (obj.foobar !== undefined) { __VERIFIER_error(); }
obj.foo = 8888;
obj.foobar = 5555;
obj.bar.foobar = 9999;

if (obj.foo !== 8888) { __VERIFIER_error(); }
if (obj.foobar !== 5555) { __VERIFIER_error(); }
if (obj.bar.foobar !== 9999) { __VERIFIER_error(); }
