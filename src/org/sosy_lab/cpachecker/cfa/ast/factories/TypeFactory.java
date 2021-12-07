// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class TypeFactory {

  @SuppressWarnings("unused")
  public static JType getMostGeneralType(JType type1, JType type2) {
    return type1;
  }

  @SuppressWarnings("unused")
  public static CType getMostGeneralType(CType type1, CType type2) {
    return type1;
  }

  @SuppressWarnings("unused")
  public static CType getCalculationType(CType type1, CType type2) {
    return type1;
  }
}
