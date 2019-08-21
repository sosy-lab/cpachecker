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
package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ToFormulaVisitor
    extends CachingVisitor<AExpression, BooleanFormula, ToFormulaException> {

  private static final CFANode DUMMY_NODE = new CFANode("dummy");

  private final FormulaManagerView formulaManagerView;

  private final PathFormulaManager pathFormulaManager;

  private final PathFormula context;

  public ToFormulaVisitor(
      FormulaManagerView pFormulaManagerView,
      PathFormulaManager pPathFormulaManager,
      PathFormula pClearContext) {
    formulaManagerView = Objects.requireNonNull(pFormulaManagerView);
    pathFormulaManager = Objects.requireNonNull(pPathFormulaManager);
    context = pClearContext;
  }

  @Override
  protected BooleanFormula cacheMissAnd(And<AExpression> pAnd) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pAnd) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().and(elements);
  }

  @Override
  protected BooleanFormula cacheMissOr(Or<AExpression> pOr) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pOr) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().or(elements);
  }

  @Override
  protected BooleanFormula cacheMissLeaf(LeafExpression<AExpression> pLeafExpression)
      throws ToFormulaException {
    AExpression expression = pLeafExpression.getExpression();
    final CFAEdge edge;
    if (expression instanceof CExpression) {
      edge = new CAssumeEdge("", FileLocation.DUMMY, DUMMY_NODE, DUMMY_NODE, (CExpression) expression, pLeafExpression.assumeTruth());
    } else if (expression instanceof JExpression) {
      edge = new JAssumeEdge("", FileLocation.DUMMY, DUMMY_NODE, DUMMY_NODE, (JExpression) expression, pLeafExpression.assumeTruth());
    } else {
      throw new AssertionError("Unsupported expression type.");
    }
    PathFormula clearContext =
        context == null
            ? pathFormulaManager.makeEmptyPathFormula()
            : pathFormulaManager.makeEmptyPathFormula(context);
    PathFormula invariantPathFormula;
    try {
      invariantPathFormula = pathFormulaManager.makeAnd(clearContext, edge);
    } catch (CPATransferException e) {
      throw new ToFormulaException(e);
    } catch (InterruptedException e) {
      throw new ToFormulaException(e);
    }
    return formulaManagerView.uninstantiate(invariantPathFormula.getFormula());
  }

  @Override
  protected BooleanFormula cacheMissTrue() {
    return formulaManagerView.getBooleanFormulaManager().makeTrue();
  }

  @Override
  protected BooleanFormula cacheMissFalse() {
    return formulaManagerView.getBooleanFormulaManager().makeFalse();
  }

  public static class ToFormulaException extends Exception {

    private static final long serialVersionUID = -3849941975554955994L;

    private final CPATransferException transferException;

    private final InterruptedException interruptedException;

    private ToFormulaException(CPATransferException pTransferException) {
      super(pTransferException);
      this.transferException = Objects.requireNonNull(pTransferException);
      this.interruptedException = null;
    }

    private ToFormulaException(InterruptedException pInterruptedException) {
      super(pInterruptedException);
      this.transferException = null;
      this.interruptedException = Objects.requireNonNull(pInterruptedException);
    }

    public boolean isTransferException() {
      return transferException != null;
    }

    public boolean isInterruptedException() {
      return interruptedException != null;
    }

    public CPATransferException asTransferException() {
      Preconditions.checkState(isTransferException());
      return transferException;
    }

    public InterruptedException asInterruptedException() {
      Preconditions.checkState(isInterruptedException());
      return interruptedException;
    }

  }

}
