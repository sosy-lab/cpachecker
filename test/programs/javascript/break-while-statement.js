function __VERIFIER_error() {}

var x = 0;

while (x !== 10) {
  x++;
  if (x === 2) {
    break;
    __VERIFIER_error();
  }
}

if (x !== 2) {
  __VERIFIER_error();
}
