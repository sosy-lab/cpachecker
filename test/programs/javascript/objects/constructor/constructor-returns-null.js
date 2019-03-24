// see https://www.ecma-international.org/ecma-262/5.1/#sec-13.2.2

function __VERIFIER_error() {}

function ChangedReturnToNull() {
  return null
}

var obj = new ChangedReturnToNull();

if (typeof obj !== 'object') {
  __VERIFIER_error();
}

if (obj === null) {
  __VERIFIER_error();
}
