// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class TypeFactory {

  public static Type getMostGeneralType(Type type1, Type type2) {
    if (type1 instanceof CType && type2 instanceof CType) {
      return CTypeFactory.getMostGeneralType((CType) type1, (CType) type2);
    } else if (type1 instanceof JType && type2 instanceof JType) {
      return JTypeFactory.getMostGeneralType((JType) type1, (JType) type2);
    } else {
      return null;
    }
  }

  @SuppressWarnings("unused")
  public static CType getCalculationType(CType type1, CType type2) {
    return type1;
  }

  public static Type getBiggestType(Type pType) {
    if (pType instanceof CType) {
      return CTypeFactory.getBiggestType((CType) pType);
    } else if (pType instanceof CType) {
      return JTypeFactory.getBiggestType((JType) pType);
    } else {
      return null;
    }
  }
}
