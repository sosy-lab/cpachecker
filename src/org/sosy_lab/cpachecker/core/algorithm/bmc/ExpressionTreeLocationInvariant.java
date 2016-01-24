/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;


public class ExpressionTreeLocationInvariant extends LocationFormulaInvariant implements ExpressionTreeCandidateInvariant {

  private final ExpressionTree<AExpression> expressionTree;

  public ExpressionTreeLocationInvariant(CFANode pLocation, ExpressionTree<AExpression> pExpressionTree) {
    super(pLocation);
    expressionTree = pExpressionTree;
  }

  @Override
  public BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws CPATransferException, InterruptedException {
    try {
      return expressionTree.accept(new ToFormulaVisitor(pFMGR, pPFMGR));
    } catch (ToFormulaException e) {
      if (e.isInterruptedException()) {
        throw e.asInterruptedException();
      }
      throw e.asTransferException();
    }
  }

  @Override
  public ExpressionTree<Object> asExpressionTree() {
    return ExpressionTrees.convert(expressionTree, new Function<AExpression, Object>() {

      @Override
      public Object apply(AExpression pExpression) {
        return pExpression;
      }

    });
  }

  @Override
  public String toString() {
    return getLocations() + ": " + expressionTree.toString();
  }

}
