function __VERIFIER_error() {}

var obj = {};
var proto = {};

if (obj.prop !== undefined) {
  __VERIFIER_error();
}

if (proto.prop !== undefined) {
  __VERIFIER_error();
}

proto.prop = 42;

if (obj.prop !== undefined) {
  __VERIFIER_error();
}

if (proto.prop !== 42) {
  __VERIFIER_error();
}

obj.__proto__ = proto;

if (obj.prop !== 42) {
  __VERIFIER_error();
}

if (proto.prop !== 42) {
  __VERIFIER_error();
}

proto.prop = 99;

if (obj.prop !== 99) {
  __VERIFIER_error();
}

if (proto.prop !== 99) {
  __VERIFIER_error();
}

obj.prop = 77;

if (obj.prop !== 77) {
  __VERIFIER_error();
}

if (proto.prop !== 99) {
  __VERIFIER_error();
}
