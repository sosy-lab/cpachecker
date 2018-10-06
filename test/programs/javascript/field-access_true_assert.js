function __VERIFIER_error() {}

var obj = {};
if (obj.prop !== undefined) {
  __VERIFIER_error();
}

obj.prop = 1;
if (obj.prop !== 1) {
  __VERIFIER_error();
}

// do assignment after (redundant) check to test if branches are merged correctly (e.g. in BMC)
if (obj.prop === obj.prop) {
  obj.prop = 2;
}
if (obj.prop !== 2) {
  __VERIFIER_error();
}

