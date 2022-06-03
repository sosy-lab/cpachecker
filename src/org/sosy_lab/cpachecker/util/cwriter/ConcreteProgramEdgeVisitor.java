// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Deque;
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

public class ConcreteProgramEdgeVisitor extends DefaultEdgeVisitor {

  private final CFAPathWithAssumptions exactValuePath;

  public ConcreteProgramEdgeVisitor(PathTranslator t, CFAPathWithAssumptions exactValues) {
    super(t);
    exactValuePath = exactValues;
  }

  @Override
  public void visit(ARGState pChildElement, CFAEdge pEdge, Deque<FunctionBody> pFunctionStack) {
    translator.processEdge(pChildElement, pEdge, pFunctionStack);

    createAssumedAssigmentString(pFunctionStack, pEdge);
  }

  private void createAssumedAssigmentString(
      Deque<FunctionBody> functionStack, CFAEdge currentCFAEdge) {
    CFAEdgeWithAssumptions e = findMatchingEdge(currentCFAEdge);
    if (e != null && e.getCFAEdge() instanceof CStatementEdge) {

      CStatementEdge cse = (CStatementEdge) e.getCFAEdge();
      // could improve detection of introductions of non-det variables
      if (!(cse.getStatement() instanceof CFunctionCallAssignmentStatement)) {
        return;
      }

      for (AExpressionStatement exp : e.getExpStmts()) {
        if (!(exp instanceof CExpressionStatement)
            || !(exp.getExpression() instanceof CBinaryExpression)) {
          continue;
        }

        CBinaryExpression cexp = (CBinaryExpression) exp.getExpression();
        if (!cexp.getOperator().equals(CBinaryExpression.BinaryOperator.EQUALS)) {
          continue;
        }

        assert cexp.getOperand1() instanceof CLeftHandSide
            : "model-refined element is not a lefthandside expression";

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
