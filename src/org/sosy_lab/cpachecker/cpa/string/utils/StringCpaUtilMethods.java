// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cpa.string.StringState;
import org.sosy_lab.cpachecker.cpa.string.domains.DomainType;

/*
 * Helper - class
 */
public class StringCpaUtilMethods {

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

  public static boolean
      methoCallEquals(JReferencedMethodInvocationExpression jrmie, StringState state) {
    return jrmie.getDeclaration().getOrigName().contains("String_equals")
        && state.getOptions().containsDomain(DomainType.STRING_SET);
  }

  public static boolean
      methoCallStartsWith(JReferencedMethodInvocationExpression jrmie, StringState state) {
    return jrmie.getDeclaration().getOrigName().contains("String_startswith")
        && state.getOptions().containsDomain(DomainType.PREFFIX);
  }

  public static boolean isTemporaryVariable(JIdExpression jidExp) {
    return jidExp.getName().contains("__CPAchecker_TMP");
  }

  public static boolean isTemporaryVariable(JVariableDeclaration jDecl) {
    return jDecl.getName().contains("__CPAchecker_TMP");
  }
}
