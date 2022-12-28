// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class JTypeFactory {

  @SuppressWarnings("unused")
  public static JType getMostGeneralType(JType type1, JType type2) {
    return type1;
  }

  public static JType getBiggestType(JType pType) {
    if (pType instanceof JSimpleType) {
      return pType;
    } else {
      return null;
    }
  }

  @SuppressWarnings("unused")
  public static Number getUpperLimit(JType pType) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Number getLowerLimit(JType pType) {
    // TODO Auto-generated method stub
    return null;
  }
}
