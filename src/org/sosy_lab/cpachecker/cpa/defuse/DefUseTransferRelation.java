/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DefUseTransferRelation implements TransferRelation
{
  private DefUseState handleExpression (DefUseState defUseState, IASTStatement expression, CFAEdge cfaEdge)
  {
    if (expression instanceof IASTAssignment)
    {
      IASTAssignment assignExpression = (IASTAssignment) expression;

      String lParam = assignExpression.getLeftHandSide().toASTString();
      // String lParam2 = binaryExpression.getOperand2 ().getRawSignature ();

      DefUseDefinition definition = new DefUseDefinition (lParam, cfaEdge);
      defUseState = new DefUseState(defUseState, definition);
    }
    return defUseState;
  }

  private DefUseState handleDeclaration (DefUseState defUseState, DeclarationEdge cfaEdge)
  {
    if (cfaEdge.getDeclaration() instanceof IASTVariableDeclaration) {
      IASTVariableDeclaration decl = (IASTVariableDeclaration)cfaEdge.getDeclaration();
      IASTInitializer initializer = decl.getInitializer();
      if (initializer != null)
      {
        String varName = decl.getName();
        DefUseDefinition definition = new DefUseDefinition (varName, cfaEdge);

        defUseState = new DefUseState(defUseState, definition);
      }
    }
    return defUseState;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState element, Precision prec, CFAEdge cfaEdge) throws CPATransferException {
    DefUseState defUseState = (DefUseState) element;

    switch (cfaEdge.getEdgeType ())
    {
    case StatementEdge:
    {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTStatement expression = statementEdge.getStatement();
      defUseState = handleExpression (defUseState, expression, cfaEdge);
      break;
    }
    case DeclarationEdge:
    {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      defUseState = handleDeclaration (defUseState, declarationEdge);
      break;
    }
    }

    return Collections.singleton(defUseState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
                         List<AbstractState> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
    return null;
  }
}
