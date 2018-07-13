function __VERIFIER_error() {}

var int = 42, float = 3.14;
var singleQuoted = 'bar', doubleQuoted = "foobar";
var truthy = true, falsy = false;
var nullable = null;

// Not working due to a bug in the Eclipse parser, see
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=518324
// var notDefined = undefined;

if (int !== 42) { __VERIFIER_error(); }
if (float !== 3.14) { __VERIFIER_error(); }
// TODO uncomment when string value tracking is implemented
// if (singleQuoted !== "bar") { __VERIFIER_error(); }
// if (doubleQuoted !== 'foobar') { __VERIFIER_error(); }
if (!truthy) { __VERIFIER_error(); }
if (falsy) { __VERIFIER_error(); }
if (nullable !== null) { __VERIFIER_error(); }
