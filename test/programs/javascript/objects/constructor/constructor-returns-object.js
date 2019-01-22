// see https://www.ecma-international.org/ecma-262/5.1/#sec-13.2.2

function __VERIFIER_error() {}

function ChangedReturnToObject() {
  return {
    prop: 42
  }
}

var obj = new ChangedReturnToObject();

if (typeof obj !== 'object') {
  __VERIFIER_error();
}

if (obj.prop !== 42) {
  __VERIFIER_error();
}
