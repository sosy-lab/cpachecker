function __VERIFIER_error() {}
function __VERIFIER_success() {}

var falsy = false;
if (falsy) {
  __VERIFIER_error();
} else {
  __VERIFIER_success();
}

var truthy = true;
if (truthy) {
  __VERIFIER_success();
} else {
  __VERIFIER_error();
}

var zero = 0;
if (zero !== 0) {
  __VERIFIER_error();
} else if (zero === 0) {
  __VERIFIER_success();
} else {
  __VERIFIER_error();
}

if (zero) {
  __VERIFIER_error();
} else {
  __VERIFIER_success();
}

var one = 1;
if (one) {
  __VERIFIER_success();
} else {
  __VERIFIER_error();
}