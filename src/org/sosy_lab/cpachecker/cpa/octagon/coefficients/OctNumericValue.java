/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class OctNumericValue {

  boolean handleFloatsAsFloats = true;

  private BigDecimal floatVal = null;
  private BigInteger intVal = null;

  private boolean isInt;

  public final static OctNumericValue ZERO = new OctNumericValue(BigInteger.ZERO);
  public final static OctNumericValue ONE = new OctNumericValue(BigInteger.ONE);

  public OctNumericValue(BigDecimal floatVal) {
    this.floatVal = floatVal;
    isInt = false;
  }

  public OctNumericValue(BigInteger intVal) {
    this.intVal = intVal;
    isInt = true;
  }

  public OctNumericValue(long intVal) {
    this.intVal = BigInteger.valueOf(intVal);
    isInt = true;
  }

  public OctNumericValue(double floatVal) {
    this.floatVal = BigDecimal.valueOf(floatVal);
    isInt = false;
  }

  public boolean isInt() {
    return isInt;
  }

  public boolean isFloat() {
    return !isInt;
  }

  public BigInteger getIntVal() {
    if (!isInt) {
      throw new IllegalStateException("This is a float value, not an int value");
    }
    return intVal;
  }

  public BigDecimal getFloatVal() {
    if (isInt) {
      throw new IllegalStateException("This is an int value, not a float value");
    }
    return floatVal;
  }

  public boolean isInInterval(double lower, double upper) {
    if (floatVal != null) {
      return floatVal.compareTo(BigDecimal.valueOf(lower)) > 0
          && floatVal.compareTo(BigDecimal.valueOf(upper)) < 0;
    } else {
      return intVal.compareTo(BigInteger.valueOf((long) Math.floor(lower))) > 0
            && intVal.compareTo(BigInteger.valueOf((long) Math.ceil(upper))) < 0;
    }
  }

  public OctNumericValue add(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return new OctNumericValue(intVal.add(val.intVal));
      } else {
        if (handleFloatsAsFloats) {
          return new OctNumericValue(val.floatVal.doubleValue() + intVal.longValue());
        } else {
          return new OctNumericValue(val.floatVal.add(BigDecimal.valueOf(intVal.longValue())));
        }
      }

    } else {
      if (handleFloatsAsFloats) {
        if (val.floatVal == null) {
          return new OctNumericValue(floatVal.doubleValue() + val.intVal.longValue());
        } else {
          return new OctNumericValue(floatVal.doubleValue() + val.floatVal.doubleValue());
        }

      } else {
        if (val.floatVal == null) {
          return new OctNumericValue(floatVal.add(BigDecimal.valueOf(val.intVal.longValue())));
        } else {
          return new OctNumericValue(floatVal.add(val.floatVal));
        }
      }
    }
  }

  public OctNumericValue subtract(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return new OctNumericValue(intVal.subtract(val.intVal));
      } else {
        return new OctNumericValue(BigDecimal.valueOf(intVal.longValue()).subtract(val.floatVal));
      }
    } else {
      if (val.floatVal == null) {
        return new OctNumericValue(floatVal.subtract(BigDecimal.valueOf(val.intVal.longValue())));
      } else {
        return new OctNumericValue(floatVal.subtract(val.floatVal));
      }
    }
  }

  public OctNumericValue mul(int val) {
    if (floatVal == null) {
      return new OctNumericValue(intVal.multiply(BigInteger.valueOf(val)));
    } else {
      return new OctNumericValue(floatVal.multiply(BigDecimal.valueOf(val)));
    }
  }

  public OctNumericValue mul(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return new OctNumericValue(intVal.multiply(val.intVal));
      } else {
        return new OctNumericValue(BigDecimal.valueOf(intVal.longValue()).multiply(val.floatVal));
      }
    } else {
      if (val.floatVal == null) {
        return new OctNumericValue(floatVal.multiply(BigDecimal.valueOf(val.intVal.longValue())));
      } else {
        return new OctNumericValue(floatVal.multiply(val.floatVal));
      }
    }
  }

  public OctNumericValue div(OctNumericValue divisor) {
    if (floatVal == null) {
      if (divisor.floatVal == null) {
        return new OctNumericValue(intVal.divide(divisor.intVal));
      } else {
        try {
          return new OctNumericValue(BigDecimal.valueOf(intVal.longValue()).divide(divisor.floatVal, 3, BigDecimal.ROUND_HALF_UP));
        } catch (ArithmeticException e) {
          System.out.println(floatVal);
        }
        return new OctNumericValue(BigDecimal.valueOf(intVal.longValue()).divide(divisor.floatVal, 3, BigDecimal.ROUND_HALF_UP));
      }
    } else {
      if (divisor.floatVal == null) {
        return new OctNumericValue(floatVal.divide(BigDecimal.valueOf(divisor.intVal.longValue())));
      } else {
        return new OctNumericValue(floatVal.divide(divisor.floatVal, 3, BigDecimal.ROUND_HALF_UP));
      }
    }
  }

  @Override
  public String toString() {
    if (floatVal == null) {
      return intVal.toString();
    } else {
      return floatVal.toString();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof OctNumericValue)) {
      return false;
    }

    OctNumericValue other = (OctNumericValue) obj;

    if (other.floatVal != null) {
      if (floatVal != null) {
        return other.floatVal.compareTo(floatVal) == 0;
      } else {
        return other.floatVal.compareTo(new BigDecimal(intVal.toString())) == 0;
      }
    } else  {
      if (floatVal != null) {
        return floatVal.compareTo(new BigDecimal(other.intVal.toString())) == 0;
      } else {
        return other.intVal.compareTo(intVal) == 0;
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(intVal);
    result = prime * result + Objects.hash(floatVal);
    return result;
  }

  public boolean greaterEqual(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return intVal.compareTo(val.intVal) >= 0;
      } else {
        return val.floatVal.compareTo(BigDecimal.valueOf(intVal.longValue())) < 0;
      }
    } else {
      if (val.floatVal == null) {
        return floatVal.compareTo(BigDecimal.valueOf(val.intVal.longValue())) >= 0;
      } else {
        return floatVal.compareTo(val.floatVal) >= 0;
      }
    }
  }

  public boolean greaterThan(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return intVal.compareTo(val.intVal) > 0;
      } else {
        return val.floatVal.compareTo(BigDecimal.valueOf(intVal.longValue())) <= 0;
      }
    } else {
      if (val.floatVal == null) {
        return floatVal.compareTo(BigDecimal.valueOf(val.intVal.longValue())) > 0;
      } else {
        return floatVal.compareTo(val.floatVal) > 0;
      }
    }
  }

    public boolean lessEqual(OctNumericValue val) {
      if (floatVal == null) {
        if (val.floatVal == null) {
          return intVal.compareTo(val.intVal) <= 0;
        } else {
          return val.floatVal.compareTo(BigDecimal.valueOf(intVal.longValue())) > 0;
        }
      } else {
        if (val.floatVal == null) {
          return floatVal.compareTo(BigDecimal.valueOf(val.intVal.longValue())) <= 0;
        } else {
          return floatVal.compareTo(val.floatVal) <= 0;
        }
      }
    }

    public boolean lessThan(OctNumericValue val) {
      if (floatVal == null) {
        if (val.floatVal == null) {
          return intVal.compareTo(val.intVal) < 0;
        } else {
          return val.floatVal.compareTo(BigDecimal.valueOf(intVal.longValue())) >= 0;
        }
      } else {
        if (val.floatVal == null) {
          return floatVal.compareTo(BigDecimal.valueOf(val.intVal.longValue())) < 0;
        } else {
          return floatVal.compareTo(val.floatVal) < 0;
        }
      }
    }
}
