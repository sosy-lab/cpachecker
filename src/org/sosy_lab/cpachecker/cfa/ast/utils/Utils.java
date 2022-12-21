// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.utils;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class Utils {

  public static boolean isNondetCall(AAstNode element) {

    if (!(element instanceof CFunctionCallExpression)) {
      return false;
    }

    CExpression functionNameExpression = ((CFunctionCallExpression) element).getFunctionNameExpression();

    if (!(functionNameExpression instanceof CIdExpression)) {
      return false;
    }

    String functionName = ((CIdExpression) functionNameExpression).getName();

    if (functionName.startsWith("__VERIFIER_nondet_")) {
      return true;
    }

    return false;
  }
}
