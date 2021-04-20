// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicateToExpressionTreeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.BuiltinCollectingVisitor;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class ACSLTransferRelation extends SingleEdgeTransferRelation {

  private final CFAWithACSLAnnotations cfa;
  private final ACSLPredicateToExpressionTreeVisitor acslVisitor;
  private final ToCExpressionVisitor expressionTreeVisitor;
  private final boolean usePureExpressionsOnly;

  public ACSLTransferRelation(
      CFAWithACSLAnnotations pCFA,
      ACSLPredicateToExpressionTreeVisitor pACSLVisitor,
      ToCExpressionVisitor pExpressionTreeVisitor,
      boolean pUsePureExpressionsOnly) {
    cfa = pCFA;
    acslVisitor = pACSLVisitor;
    expressionTreeVisitor = pExpressionTreeVisitor;
    usePureExpressionsOnly = pUsePureExpressionsOnly;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    Set<ACSLAnnotation> annotationsForEdge =
        ImmutableSet.copyOf(cfa.getEdgesToAnnotations().get(cfaEdge));
    if (usePureExpressionsOnly) {
      BuiltinCollectingVisitor visitor = new BuiltinCollectingVisitor();
      Set<ACSLAnnotation> annotations =
          FluentIterable.from(annotationsForEdge)
              .filter(x -> x.getPredicateRepresentation().accept(visitor).isEmpty())
              .toSet();
      return ImmutableList.of(new ACSLState(annotations, acslVisitor, expressionTreeVisitor));
    }
    return ImmutableList.of(new ACSLState(annotationsForEdge, acslVisitor, expressionTreeVisitor));
  }
}
