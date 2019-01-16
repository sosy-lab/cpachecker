function __VERIFIER_error() {}

if (typeof true !== 'boolean') {
  __VERIFIER_error();
}

if (typeof false !== 'boolean') {
  __VERIFIER_error();
}

var truthy = true;
if (typeof truthy !== 'boolean') {
  __VERIFIER_error();
}

var falsy = false;
if (typeof falsy !== 'boolean') {
  __VERIFIER_error();
}
