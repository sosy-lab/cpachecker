/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Stack;


public class ConcreteProgramEdgeVisitor extends DefaultEdgeVisitor {

  private final CFAPathWithAssumptions exactValuePath;

  public ConcreteProgramEdgeVisitor(PathTranslator t, CFAPathWithAssumptions exactValues) {
    super(t);
    exactValuePath = exactValues;
  }

  @Override
  public void visit(ARGState pChildElement, CFAEdge pEdge, Stack<FunctionBody> pFunctionStack) {
    translator.processEdge(pChildElement, pEdge, pFunctionStack);

    createAssumedAssigmentString(pFunctionStack, pEdge);
  }


  private void createAssumedAssigmentString(Stack<FunctionBody> functionStack,
      CFAEdge currentCFAEdge) {
    CFAEdgeWithAssumptions e = findMatchingEdge(currentCFAEdge);
    if (e != null &&
        e.getCFAEdge() instanceof CStatementEdge) {

      CStatementEdge cse = (CStatementEdge) e.getCFAEdge();
      // could improve detection of introductions of non-det variables
      if (!(cse.getStatement() instanceof CFunctionCallAssignmentStatement)) { return; }

      for (AExpressionStatement exp : e.getExpStmts()) {
        if (!(exp instanceof CExpressionStatement) ||
            !(exp.getExpression() instanceof CBinaryExpression)) {
          continue;
        }

        CBinaryExpression cexp = (CBinaryExpression) exp.getExpression();
        if (!cexp.getOperator().equals(CBinaryExpression.BinaryOperator.EQUALS)) {
          continue;
        }

        assert cexp.getOperand1() instanceof CLeftHandSide : "model-refined element is not a lefthandside expression";

        CLeftHandSide lho = ((CLeftHandSide) cexp.getOperand1());

        if (lho.getExpressionType() instanceof CPointerType) {
          // don't mess up pointer addresses in generated program
          // void * ptr = 1LL will segfault ;(
          continue;
        }

        CExpressionAssignmentStatement cass =
            new CExpressionAssignmentStatement(cexp.getFileLocation(), lho, cexp.getOperand2());


        functionStack.peek().write(cass.toASTString());
      }
    }
  }

  private CFAEdgeWithAssumptions findMatchingEdge(CFAEdge e) {
    for (CFAEdgeWithAssumptions edgeWithAssignments : from(exactValuePath).filter(notNull())) {
      if (e.equals(edgeWithAssignments.getCFAEdge())) {
        return edgeWithAssignments;
      }
    }

    return null;
  }


}
