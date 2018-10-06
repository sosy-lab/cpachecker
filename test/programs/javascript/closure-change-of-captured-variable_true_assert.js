// change of captured variable should not influence captured variable of another closure instance
function __VERIFIER_error() {}

function counter() {
  var count = 0;
  return function increment() {
    ++count;
    return count;
  }
}

var a1 = counter();
var b1 = counter();

if (a1() !== 1) { __VERIFIER_error(); }
if (a1() !== 2) { __VERIFIER_error(); }

if (b1() !== 1) { __VERIFIER_error(); }
if (b1() !== 2) { __VERIFIER_error(); }

if (a1() !== 3) { __VERIFIER_error(); }
if (b1() !== 3) { __VERIFIER_error(); }
