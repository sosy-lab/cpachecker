// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CTypeFactory {

  // TODO: Find an Immutable equivalent of Number
  @SuppressWarnings("ImmutableEnumChecker")
  private enum TypeBounds {
    // According to: https://en.cppreference.com/w/cpp/types/climits
    CHAR(127, -128),
    SCHAR(127, -128),
    SHRT(32767, -32768),
    INT(2147483647, -2147483648),
    LONG(9223372036854775807L, -9223372036854775808L),
    LLONG(9223372036854775807L, -9223372036854775808L),
    UCHAR(255, 0),
    USHRT(65535L, 0),
    UINT(4294967295L, 0),
    ULONG(new BigInteger("18446744073709551615"), 0),
    ULLONG(new BigInteger("18446744073709551615"), 0),
    FLT(1.17549e-38, 3.40282e+38),
    DBL(2.22507e-308, 1.79769e+308),
    LDBL(new BigDecimal("3.3621e-4932"), new BigDecimal("1.18973e+4932"))
    ;

    private final Number upperBound;
    private final Number lowerBound;

    TypeBounds(Number pUpperBound, Number pLowerBound) {
      upperBound = pUpperBound;
      lowerBound = pLowerBound;
    }

    public Number getUpperBound() {
      return upperBound;
    }

    public Number getLowerBound() {
      return lowerBound;
    }

  }


  public static CType getMostGeneralType(CType type1, CType type2) {
    if (type1 instanceof CSimpleType && type2 instanceof CSimpleType) {
      switch (((CSimpleType) type1).getType()) {
        case BOOL:
        case CHAR:
        case DOUBLE:
          return type1;
        case FLOAT:
        case FLOAT128:
          if (((CSimpleType) type2).getType() == CBasicType.DOUBLE) {
            return type2;
          } else {
            return type1;
          }
        case INT:
        case INT128:
          return new CSimpleType(
              false, false, CBasicType.INT, false, false, false, false, false, false, true);
        case UNSPECIFIED:
        default:
          return type1;
      }
    } else {
      return null;
    }
  }

  public static CType getBiggestType(CType pType) {
    if (pType instanceof CSimpleType) {
      switch (((CSimpleType) pType).getType()) {
        case BOOL:
        case CHAR:
        case UNSPECIFIED:
        case DOUBLE:
          return pType;
        case FLOAT:
        case FLOAT128:
          return new CSimpleType(
              false, false, CBasicType.DOUBLE, false, false, false, false, false, false, false);
        case INT:
        case INT128:
          return new CSimpleType(
              false, false, CBasicType.INT, false, false, false, false, false, false, true);
        default:
          return pType;
      }
    } else {
      return null;
    }
  }

  public static Number getUpperLimit(CType pType) {
    if (pType instanceof CSimpleType) {
      if (((CSimpleType) pType).isLong()) {
        if(((CSimpleType)pType).isUnsigned()) {
          return TypeBounds.ULONG.getUpperBound();
        } else {
          return TypeBounds.LONG.getUpperBound();
        }
      } else if (((CSimpleType) pType).isLongLong()) {
        if(((CSimpleType)pType).isUnsigned()) {
          return TypeBounds.ULLONG.getUpperBound();
        } else {
          return TypeBounds.LLONG.getUpperBound();
        }
      } else if (((CSimpleType) pType).isShort()) {
        if (((CSimpleType) pType).isUnsigned()) {
          return TypeBounds.USHRT.getUpperBound();
        } else {
          return TypeBounds.SHRT.getUpperBound();
        }
      }

      switch (((CSimpleType) pType).getType()) {
        case BOOL:
          return 1;
        case CHAR:
          return 127;
        case DOUBLE:
          return null;
        case FLOAT:
          return null;
        case FLOAT128:
          return null;
        case INT:
          return 2147483647;
        case INT128:
          return null;
        case UNSPECIFIED:
        default:
          return null;
      }
    } else if (pType instanceof AArrayType) {
      // TODO: When checking if it is an instance of CArrayType this produces false. For example for
      // test/programs/benchmarks/reducercommutativity/avg05-1.i
      return getUpperLimit((CType) ((AArrayType) pType).getType());
    } else {
      return null;
    }
  }

  public static Number getLowerLimit(CType pType) {

    if (pType instanceof CSimpleType) {
      if (((CSimpleType) pType).isLong()) {
        if(((CSimpleType)pType).isUnsigned()) {
          return TypeBounds.ULONG.getLowerBound();
        } else {
          return TypeBounds.LONG.getLowerBound();
        }
      } else if (((CSimpleType) pType).isLongLong()) {
        if(((CSimpleType)pType).isUnsigned()) {
          return TypeBounds.ULLONG.getLowerBound();
        } else {
          return TypeBounds.LLONG.getLowerBound();
        }
      } else if (((CSimpleType) pType).isShort()) {
        if (((CSimpleType) pType).isUnsigned()) {
          return TypeBounds.USHRT.getLowerBound();
        } else {
          return TypeBounds.SHRT.getLowerBound();
        }
      }

      switch (((CSimpleType) pType).getType()) {
        case BOOL:
          return 0;
        case CHAR:
          return -128;
        case DOUBLE:
          return null;
        case FLOAT:
          return null;
        case FLOAT128:
          return null;
        case INT:
          return -2147483648;
        case INT128:
          return null;
        case UNSPECIFIED:
        default:
          return null;
      }
    } else if (pType instanceof AArrayType) {
      return getLowerLimit((CType) ((AArrayType) pType).getType());
    } else {
      return null;
    }
  }
}
