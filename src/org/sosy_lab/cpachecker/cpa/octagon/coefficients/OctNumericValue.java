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

  private BigDecimal floatVal = null;
  private BigInteger intVal = null;

  public final static OctNumericValue ZERO = new OctNumericValue(BigInteger.ZERO);
  public final static OctNumericValue ONE = new OctNumericValue(BigInteger.ONE);

  public OctNumericValue(BigDecimal floatVal) {
    this.floatVal = floatVal;
  }

  public OctNumericValue(BigInteger intVal) {
    this.intVal = intVal;
  }

  public OctNumericValue(long intVal) {
    this.intVal = BigInteger.valueOf(intVal);
  }

  public OctNumericValue(double floatVal) {
    this.floatVal = BigDecimal.valueOf(floatVal);
  }

  public boolean isInt() {
    return intVal != null;
  }

  public boolean isFloat() {
    return floatVal != null;
  }

  public BigInteger getIntVal() {
    if (intVal == null) {
      throw new IllegalStateException("This is a float value, not an int value");
    }
    return intVal;
  }

  public BigDecimal getFloatVal() {
    if (floatVal == null) {
      throw new IllegalStateException("This is an int value, not a float value");
    }
    return floatVal;
  }

  public OctNumericValue add(OctNumericValue val) {
    if (floatVal == null) {
      if (val.floatVal == null) {
        return new OctNumericValue(intVal.add(val.intVal));
      } else {
        return new OctNumericValue(val.floatVal.add(BigDecimal.valueOf(intVal.longValue())));
      }
    } else {
      if (val.floatVal == null) {
        return new OctNumericValue(floatVal.add(BigDecimal.valueOf(val.intVal.longValue())));
      } else {
        return new OctNumericValue(floatVal.add(val.floatVal));
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

    if (other.floatVal != null && floatVal != null) {
      return other.floatVal.equals(floatVal);
    } else if (other.intVal != null && intVal != null) {
      return other.intVal.equals(intVal);
    } else {
      return false;
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
