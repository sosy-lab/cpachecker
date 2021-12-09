// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cpa.string.StringState;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;

/*
 * Helper - class
 */
public class HelperMethods {

  public static boolean isString(Type pType) {

    if (pType instanceof JClassType) {

      JClassType t = (JClassType) pType;

      if (t.getName().equals("java.lang.String") && t.getSimpleName().equals("String")) {
        return true;
      }

    }

    return false;
  }

  public static boolean
      methodCallLength(JReferencedMethodInvocationExpression jrmie, StringState state) {
    return jrmie.getDeclaration().getOrigName().contains("String_length")
        && state.getOptions().containsDomain(DomainType.LENGTH);
  }
}
