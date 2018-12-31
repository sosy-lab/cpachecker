function __VERIFIER_error() {}

// Benchmark runs with
//   -setprop cpa.predicate.js.maxPrototypeChainLength=5
// Thereby, this prototype chain of length 5 is encoded accurately in the formulas.
var obj = {
  __proto__: {
    __proto__: {
      __proto__: {
        __proto__: {
          __proto__: {
            prop: 42
          }
        }
      }
    }
  }
};

// Expected verifier result is TRUE, since prototype chain is encoded accurately in the formulas.
if (obj.prop !== 42) {
  __VERIFIER_error();
}
