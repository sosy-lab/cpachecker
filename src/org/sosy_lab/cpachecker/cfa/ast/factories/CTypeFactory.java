// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CTypeFactory {

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
    } else {
      return null;
    }
  }

  public static Number getLowerLimit(CType pType) {
    if (pType instanceof CSimpleType) {
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
    } else {
      return null;
    }
  }
}
