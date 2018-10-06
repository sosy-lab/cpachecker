function __VERIFIER_error() {}

if ({} === {}) {
  __VERIFIER_error();
}

if (null === {}) {
  __VERIFIER_error();
}

if ({} === null) {
  __VERIFIER_error();
}

if (null !== null) {
  __VERIFIER_error();
}

var nullable = null;
if (nullable !== null) {
  __VERIFIER_error();
}

var a = {};
if (a !== a) {
  __VERIFIER_error();
}

var b = {};
if (a === b) {
  __VERIFIER_error();
}