function __VERIFIER_error() {}

g = 999;

if (g !== 999) {
  __VERIFIER_error();
}

function changeGlobal() {
  g = 888;
}

changeGlobal();

if (g !== 888) {
  __VERIFIER_error();
}

function localVarHidesGlobal() {
  var g = 777;
  if (g !== 777) {
    __VERIFIER_error();
  }
}

localVarHidesGlobal();

// global var is not changed
if (g !== 888) {
  __VERIFIER_error();
}

function parameterHidesGlobal(g) {
  if (g !== 666) {
    __VERIFIER_error();
  }
}

parameterHidesGlobal(666);

// global var is not changed
if (g !== 888) {
  __VERIFIER_error();
}
