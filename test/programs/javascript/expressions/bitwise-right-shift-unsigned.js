function __VERIFIER_error() {}

if ((9 >>> 2) !== 2) {
  __VERIFIER_error();
}

if ((9.7 >>> 2) !== 2) {
  __VERIFIER_error();
}

if ((-9 >>> 2) !== 1073741821) {
  __VERIFIER_error();
}

if ((-9 >>> 2.3) !== 1073741821) {
  __VERIFIER_error();
}
