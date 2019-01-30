function __VERIFIER_error() {}

var x = 0;
var y = 0;

outer:
    do {
      ++x;
      do {
        ++y;
        continue outer;
        __VERIFIER_error();
      } while (y < 10);
    } while (x < 3);

if (x !== 3) {
  __VERIFIER_error();
}

if (y !== 3) {
  __VERIFIER_error();
}
