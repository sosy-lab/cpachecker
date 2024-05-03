// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Ascii;
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
    return o instanceof ACSLType other && typeName.equals(other.getTypeName());
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
    return switch (Ascii.toLowerCase(typeName)) {
      case "_bool" -> CNumericTypes.BOOL;
      case "char" -> CNumericTypes.CHAR;
      case "signed char" -> CNumericTypes.SIGNED_CHAR;
      case "unsigned char" -> CNumericTypes.UNSIGNED_CHAR;
      case "int" -> CNumericTypes.INT;
      case "signed", "signed int" -> CNumericTypes.SIGNED_INT;
      case "unsigned", "unsigned int" -> CNumericTypes.UNSIGNED_INT;
      case "short", "short int", "signed short", "signed short int" -> CNumericTypes.SHORT_INT;
      case "unsigned short", "unsigned short int" -> CNumericTypes.UNSIGNED_SHORT_INT;
      case "long", "long int" -> CNumericTypes.LONG_INT;
      case "signed long", "signed long int" -> CNumericTypes.SIGNED_LONG_INT;
      case "unsigned long", "unsigned long int" -> CNumericTypes.UNSIGNED_LONG_INT;
      case "long long", "long long int" -> CNumericTypes.LONG_LONG_INT;
      case "signed long long", "signed long long int" -> CNumericTypes.SIGNED_LONG_LONG_INT;
      case "unsigned long long", "unsigned long long int" -> CNumericTypes.UNSIGNED_LONG_LONG_INT;
      case "float" -> CNumericTypes.FLOAT;
      case "double" -> CNumericTypes.DOUBLE;
      case "long double" -> CNumericTypes.LONG_DOUBLE;
      default -> new CProblemType(typeName);
    };
  }
}
