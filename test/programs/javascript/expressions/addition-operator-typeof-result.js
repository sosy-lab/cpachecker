function __VERIFIER_error() {}

var num = 42;
var str = 'text';
var bool = true;

if (typeof (1 + 2) !== 'number') {
  __VERIFIER_error();
}

if (typeof (1 + num) !== 'number') {
  __VERIFIER_error();
}

if (typeof (num + 1) !== 'number') {
  __VERIFIER_error();
}

if (typeof (num + num) !== 'number') {
  __VERIFIER_error();
}

if (typeof (false + num) !== 'number') {
  __VERIFIER_error();
}

if (typeof (num + false) !== 'number') {
  __VERIFIER_error();
}

if (typeof (bool + num) !== 'number') {
  __VERIFIER_error();
}

if (typeof (num + bool) !== 'number') {
  __VERIFIER_error();
}


if (typeof ('foo' + 'bar') !== 'string') {
  __VERIFIER_error();
}

if (typeof ('foo' + num) !== 'string') {
  __VERIFIER_error();
}

if (typeof (num + 'foo') !== 'string') {
  __VERIFIER_error();
}

if (typeof (str + str) !== 'string') {
  __VERIFIER_error();
}

if (typeof (false + str) !== 'string') {
  __VERIFIER_error();
}

if (typeof (str + false) !== 'string') {
  __VERIFIER_error();
}

if (typeof (bool + str) !== 'string') {
  __VERIFIER_error();
}

if (typeof (str + bool) !== 'string') {
  __VERIFIER_error();
}

if (typeof (num + str) !== 'string') {
  __VERIFIER_error();
}

if (typeof (str + num) !== 'string') {
  __VERIFIER_error();
}
