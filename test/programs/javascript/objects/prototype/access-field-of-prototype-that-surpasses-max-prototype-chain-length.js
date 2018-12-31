function __VERIFIER_error() {}

// Benchmark runs with
//   -setprop cpa.predicate.js.maxPrototypeChainLength=5
// Thereby, this prototype chain of length 6 is encoded inaccurately in the formulas.
// Only properties up to the fifth prototype are encoded accurately.
// That's why the property `prop` of the sixth prototype is not looked up (even so it would in a
// real execution of the program).
var obj = {
  __proto__: {
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
  }
};

// Expected verifier result is FALSE (even though it should actually be TRUE),
// since prototype chain is encoded inaccurately in the formulas and `obj.prop` is seen as
// `undefined` by the verifier.
// This kind of false negative is accepted right now.
if (obj.prop !== 42) {
  __VERIFIER_error();
}
