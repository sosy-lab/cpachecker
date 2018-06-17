function __VERIFIER_error() {}

var x = 0;
var y = 0;
while (x < 3) {
  x++;
  if (x === 2) {
    continue;
    __VERIFIER_error();
  }
  y++;
}

if (x !== 3) {
  __VERIFIER_error();
}

if (y !== 2) {
  __VERIFIER_error();
}
