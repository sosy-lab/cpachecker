function __VERIFIER_error() {}

var obj = {
  foo: 6666,
  bar: 7777
};
if (obj.foo !== 6666) { __VERIFIER_error(); }
if (obj.bar !== 7777) { __VERIFIER_error(); }
if (obj.foobar !== undefined) { __VERIFIER_error(); }
obj.foo = 8888;

if (obj.foo !== 8888) { __VERIFIER_error(); }
if (obj.bar !== 7777) { __VERIFIER_error(); }
if (obj.foobar !== undefined) { __VERIFIER_error(); }
