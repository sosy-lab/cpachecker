// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class ACSLType {
  private final String typeName;

  public ACSLType(String name) {
    typeName = name;
  }

  public String getTypeName() {
    return typeName;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ACSLType) {
      ACSLType other = (ACSLType) o;
      return typeName.equals(other.getTypeName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 17 * typeName.hashCode() * typeName.hashCode();
  }

  @Override
  public String toString() {
    return typeName;
  }

  public CType toCType() {
    switch (typeName.toLowerCase()) {
      case "_bool":
        return CNumericTypes.BOOL;
      case "char":
        return CNumericTypes.CHAR;
      case "signed char":
        return CNumericTypes.SIGNED_CHAR;
      case "unsigned char":
        return CNumericTypes.UNSIGNED_CHAR;
      case "int":
        return CNumericTypes.INT;
      case "signed":
      case "signed int":
        return CNumericTypes.SIGNED_INT;
      case "unsigned":
      case "unsigned int":
        return CNumericTypes.UNSIGNED_INT;
      case "short":
      case "short int":
      case "signed short":
      case "signed short int":
        return CNumericTypes.SHORT_INT;
      case "unsigned short":
      case "unsigned short int":
        return CNumericTypes.UNSIGNED_SHORT_INT;
      case "long":
      case "long int":
        return CNumericTypes.LONG_INT;
      case "signed long":
      case "signed long int":
        return CNumericTypes.SIGNED_LONG_INT;
      case "unsigned long":
      case "unsigned long int":
        return CNumericTypes.UNSIGNED_LONG_INT;
      case "long long":
      case "long long int":
        return CNumericTypes.LONG_LONG_INT;
      case "signed long long":
      case "signed long long int":
        return CNumericTypes.SIGNED_LONG_LONG_INT;
      case "unsigned long long":
      case "unsigned long long int":
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      case "float":
        return CNumericTypes.FLOAT;
      case "double":
        return CNumericTypes.DOUBLE;
      case "long double":
        return CNumericTypes.LONG_DOUBLE;
      default:
        return new CProblemType(typeName);
    }
  }
}
