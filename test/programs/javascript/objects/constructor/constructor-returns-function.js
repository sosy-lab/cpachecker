// see https://www.ecma-international.org/ecma-262/5.1/#sec-13.2.2

function __VERIFIER_error() {}

function ChangedReturnToFunction() {
  return function () {
    return 42;
  }
}

var fun = new ChangedReturnToFunction();

if (typeof fun !== 'function') {
  __VERIFIER_error();
}

if (fun() !== 42) {
  __VERIFIER_error();
}
