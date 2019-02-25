function isNaN(value) {
  return value === undefined || value !== value;
}

function Number(value) {
  return +value;
}

Number.NaN = NaN;
Number.POSITIVE_INFINITY = Infinity;
Number.NEGATIVE_INFINITY = -Infinity;
Number.MAX_VALUE = 1.7976931348623157E308;
Number.MIN_VALUE = 4.9E-324;
