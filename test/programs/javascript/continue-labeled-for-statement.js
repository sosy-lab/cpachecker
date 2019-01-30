function __VERIFIER_error() {}

var x;
var y;

outer:
    for (x = 0; x < 3; ++x) {
      for (y = 0; y < 10; ++y) {
        continue outer;
        __VERIFIER_error();
      }
    }

if (x !== 3) {
  __VERIFIER_error();
}

if (y !== 0) {
  __VERIFIER_error();
}
