/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLPredicate;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLToCExpressionVisitor;
import org.sosy_lab.cpachecker.core.algorithm.acsl.FunctionContract;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class ACSLState implements AbstractState, ExpressionTreeReportingState {

  private final ImmutableSet<ACSLAnnotation> annotations;
  private final ACSLToCExpressionVisitor visitor;

  public ACSLState(Set<ACSLAnnotation> pAnnotations, ACSLToCExpressionVisitor pVisitor) {
    annotations = ImmutableSet.copyOf(pAnnotations);
    visitor = pVisitor;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) {
    if (annotations.isEmpty()) {
      return ExpressionTrees.getFalse();
    }
    List<ExpressionTree<Object>> representations = new ArrayList<>(annotations.size());
    for (ACSLAnnotation annotation : annotations) {
      ACSLPredicate predicate;
      if (annotation instanceof FunctionContract) {
        FunctionContract functionContract = (FunctionContract) annotation;
        if (pLocation instanceof FunctionExitNode) {
          predicate = functionContract.getPostStateRepresentation();
        } else {
          predicate = functionContract.getPreStateRepresentation();
        }
      } else {
        predicate = annotation.getPredicateRepresentation();
      }
      representations.add(predicate.toExpressionTree(visitor));
    }
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    return factory.and(representations);
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
