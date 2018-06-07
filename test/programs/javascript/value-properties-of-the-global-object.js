// Value Properties of the Global Object
// https://www.ecma-international.org/ecma-262/5.1/#sec-15.1.1
// TODO check attributes are fulfilled (e.g. property is not writable), but shadowing is possible

function __VERIFIER_error() {}

function isNaN(value) {
  return value !== value;
}

// NaN, see https://www.ecma-international.org/ecma-262/5.1/#sec-15.1.1.1
if (!isNaN(NaN)) { __VERIFIER_error(); }
if (isNaN(0)) { __VERIFIER_error(); }

// Infinity, see https://www.ecma-international.org/ecma-262/5.1/#sec-15.1.1.2
if (Infinity !== (9999999 / 0)) { __VERIFIER_error(); }

// undefined, see https://www.ecma-international.org/ecma-262/5.1/#sec-15.1.1.3
if (undefined !== void 0) { __VERIFIER_error(); }
