function __VERIFIER_error() {}

var a = [];
var indexOfSecondElement = 3 - 2;

if (a.length !== 0) {
  __VERIFIER_error();
}

if (a[0] !== undefined) {
  __VERIFIER_error();
}

if (a[indexOfSecondElement] !== undefined) {
  __VERIFIER_error();
}

a[0] = 111;

if (a[0] !== 111) {
  __VERIFIER_error();
}

if (a.length !== 1) {
  __VERIFIER_error();
}

a[indexOfSecondElement] = 999;

if (a[indexOfSecondElement] !== 999) {
  __VERIFIER_error();
}

a[0] = 222;

if (a[0] !== 222) {
  __VERIFIER_error();
}

a[indexOfSecondElement] = 888;

if (a[indexOfSecondElement] !== 888) {
  __VERIFIER_error();
}
