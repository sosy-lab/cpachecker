// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLPredicate;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLTermToCExpressionVisitor;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class ACSLState implements AbstractState, ExpressionTreeReportingState {

  private final ImmutableSet<ACSLAnnotation> annotations;
  private final ACSLTermToCExpressionVisitor visitor;

  public ACSLState(Set<ACSLAnnotation> pAnnotations, ACSLTermToCExpressionVisitor pVisitor) {
    annotations = ImmutableSet.copyOf(pAnnotations);
    visitor = pVisitor;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) {
    if (annotations.isEmpty()) {
      return ExpressionTrees.getTrue();
    }
    List<ExpressionTree<Object>> representations = new ArrayList<>(annotations.size());
    for (ACSLAnnotation annotation : annotations) {
      ACSLPredicate predicate = annotation.getPredicateRepresentation();
      representations.add(predicate.toExpressionTree(visitor));
    }
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    return factory.and(representations);
  }

  public boolean hasAnnotations() {
    return !annotations.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof ACSLState) {
      ACSLState that = (ACSLState) pO;
      return annotations.equals(that.annotations);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ACSLState " + annotations.toString();
  }
}
