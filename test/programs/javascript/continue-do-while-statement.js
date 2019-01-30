function __VERIFIER_error() {}

var x = 0;
var y = 0;
do {
  x++;
  if (x === 2) {
    continue;
    __VERIFIER_error();
  }
  y++;
} while (x < 3);

if (x !== 3) {
  __VERIFIER_error();
}

if (y !== 2) {
  __VERIFIER_error();
}
