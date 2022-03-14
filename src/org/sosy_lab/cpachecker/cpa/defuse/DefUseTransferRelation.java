// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DefUseTransferRelation extends SingleEdgeTransferRelation {
  private DefUseState handleExpression(
      DefUseState defUseState, CStatement expression, CFAEdge cfaEdge) {
    if (expression instanceof CAssignment) {
      CAssignment assignExpression = (CAssignment) expression;

      String lParam = assignExpression.getLeftHandSide().toASTString();
      // String lParam2 = binaryExpression.getOperand2 ().getRawSignature ();

      DefUseDefinition definition = new DefUseDefinition(lParam, cfaEdge);
      defUseState = new DefUseState(defUseState, definition);
    }
    return defUseState;
  }

  private DefUseState handleDeclaration(DefUseState defUseState, CDeclarationEdge cfaEdge) {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration) cfaEdge.getDeclaration();
      CInitializer initializer = decl.getInitializer();
      if (initializer != null) {
        String varName = decl.getName();
        DefUseDefinition definition = new DefUseDefinition(varName, cfaEdge);

        defUseState = new DefUseState(defUseState, definition);
      }
    }
    return defUseState;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) throws CPATransferException {
    DefUseState defUseState = (DefUseState) element;

    switch (cfaEdge.getEdgeType()) {
      case StatementEdge:
        {
          CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
          CStatement expression = statementEdge.getStatement();
          defUseState = handleExpression(defUseState, expression, cfaEdge);
          break;
        }
      case DeclarationEdge:
        {
          CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
          defUseState = handleDeclaration(defUseState, declarationEdge);
          break;
        }
      default:
        // not relevant for def-use
        break;
    }

    return Collections.singleton(defUseState);
  }
}
