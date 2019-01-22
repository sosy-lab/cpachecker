function __VERIFIER_error() {}

var num = 42;
var str = 'text';
var bool = true;

if (1 + 2 !== 3) {
  __VERIFIER_error();
}

if (1 + num !== 43) {
  __VERIFIER_error();
}

if (num + 1 !== 43) {
  __VERIFIER_error();
}

if (num + num !== 84) {
  __VERIFIER_error();
}

if (false + num !== 42) {
  __VERIFIER_error();
}

if (num + false !== 42) {
  __VERIFIER_error();
}

if (bool + num !== 43) {
  __VERIFIER_error();
}

if (num + bool !== 43) {
  __VERIFIER_error();
}


if (('foo' + 'bar') !== ('foo' + 'bar')) {
  __VERIFIER_error();
}

if (('foo' + num) !== ('foo' + num)) {
  __VERIFIER_error();
}

if ((num + 'foo') !== (num + 'foo')) {
  __VERIFIER_error();
}

if ((str + str) !== (str + str)) {
  __VERIFIER_error();
}

if ((false + str) !== (false + str)) {
  __VERIFIER_error();
}

if ((str + false) !== (str + false)) {
  __VERIFIER_error();
}

if ((bool + str) !== (bool + str)) {
  __VERIFIER_error();
}

if ((str + bool) !== (str + bool)) {
  __VERIFIER_error();
}

if ((num + str) !== (num + str)) {
  __VERIFIER_error();
}

if ((str + num) !== (str + num)) {
  __VERIFIER_error();
}

if ((str + num) !== ('text' + num)) {
  __VERIFIER_error();
}

if ((str + ('foo' + str)) !== (str + ('foo' + str))) {
  __VERIFIER_error();
}
