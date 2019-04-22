function isNaN(value) {
  return value === undefined || value !== value;
}

// constructor call not supported yet
function Number(value) {
  return +value;
}

function Boolean(value) {
  return !!value;
}

Number.NaN = NaN;
Number.POSITIVE_INFINITY = Infinity;
Number.NEGATIVE_INFINITY = -Infinity;
Number.MAX_VALUE = 1.7976931348623157E308;
Number.MIN_VALUE = 4.9E-324;

// constructor call not supported yet
function Date() {
  // dummy value
  return "Mon Feb 25 2019 12:06:04 GMT+0100 (Central European Standard Time)";
}