function __VERIFIER_error() {}

function incrementBy(amount) {
  return function increment(value) {
    return value + amount;
  }
}

var incrementByOne = incrementBy(1);
var incrementByTwo = incrementBy(2);

if (incrementByOne(1) !== 2) { __VERIFIER_error(); }
if (incrementByOne(2) !== 3) { __VERIFIER_error(); }

if (incrementByTwo(1) !== 3) { __VERIFIER_error(); }
if (incrementByTwo(2) !== 4) { __VERIFIER_error(); }
