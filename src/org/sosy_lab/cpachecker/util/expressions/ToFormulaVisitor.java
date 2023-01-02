// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

public class ToFormulaVisitor
    extends CachingVisitor<AExpression, BooleanFormula, ToFormulaException> {

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
      edge =
          new CAssumeEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              (CExpression) expression,
              pLeafExpression.assumeTruth());
    } else if (expression instanceof JExpression) {
      edge =
          new JAssumeEdge(
              "",
              FileLocation.DUMMY,
              CFANode.newDummyCFANode(),
              CFANode.newDummyCFANode(),
              (JExpression) expression,
              pLeafExpression.assumeTruth());
    } else {
      throw new AssertionError("Unsupported expression type.");
    }
    PathFormula clearContext =
        context == null
            ? pathFormulaManager.makeEmptyPathFormula()
            : pathFormulaManager.makeEmptyPathFormulaWithContextFrom(context);
    PathFormula invariantPathFormula;
    try {
      invariantPathFormula = pathFormulaManager.makeAnd(clearContext, edge);
    } catch (InterruptedException | CPATransferException e) {
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

  /**
   * An exception that wraps either an {@link InterruptedException} or a {@link
   * CPATransferException}.
   */
  public static class ToFormulaException extends Exception {

    private static final long serialVersionUID = -3849941975554955994L;

    private ToFormulaException(Exception pCause) {
      super(pCause);
    }
  }
}
