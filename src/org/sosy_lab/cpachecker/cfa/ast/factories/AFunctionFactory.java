// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class AFunctionFactory {

  private CFunctionFactory cFunctionFactory = new CFunctionFactory();
  private JFunctionFactory jFunctionFactory = new JFunctionFactory();

  public AFunctionCallExpression callNondetFunction(Type pType) {
    if (pType instanceof CType) {
      return this.cFunctionFactory.callNondetFunction((CType) pType);
    } else if (pType instanceof JType) {
      return this.jFunctionFactory.callNondetFunction((JType) pType);
    } else {
      return null;
    }
  }

  public AFunctionDeclaration declareNondetFunction(Type pType) {
    if (pType instanceof CType) {
      return this.cFunctionFactory.declareNondetFunction((CType) pType);
    } else if (pType instanceof JType) {
      return this.jFunctionFactory.declareNondetFunction((JType) pType);
    } else {
      return null;
    }
  }

  public boolean isUserDefined(AFunctionCallExpression pFunctionCall) {
    if (pFunctionCall instanceof CFunctionCallExpression) {
      return this.cFunctionFactory.isUserDefined((CFunctionCallExpression) pFunctionCall);
    } else {
      // TODO: Consider this for Java programs
      return true;
    }
  }
}
