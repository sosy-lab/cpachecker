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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ReachedSetBasedExpressionTreeSupplier implements ExpressionTreeSupplier {

  private final LazyLocationMapping lazyLocationMapping;
  private final CFA cfa;

  public ReachedSetBasedExpressionTreeSupplier(LazyLocationMapping pLazyLocationMapping, CFA pCFA) {
    lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    cfa = Objects.requireNonNull(pCFA);
  }

  @Override
  public ExpressionTree<Object> getInvariantFor(CFANode pLocation) {
    ExpressionTree<Object> locationInvariant = ExpressionTrees.getFalse();

    Set<InvariantsState> invStates = Sets.newHashSet();
    boolean otherReportingStates = false;
    Iterable<AbstractState> locationStates = lazyLocationMapping.get(
        pLocation,
        Optional.empty());
    if (Iterables.isEmpty(locationStates)) {
      // Location is not necessarily unreachable, but might have been skipped,
      // e.g. by the multi-edge/basic-block optimization of the CompositeCPA.
      // In this case, we err on the side of caution.
      return ExpressionTrees.getTrue();
    }

    for (AbstractState locState : locationStates) {
      ExpressionTree<Object> stateInvariant = ExpressionTrees.getTrue();

      for (ExpressionTreeReportingState expressionTreeReportingState :
          AbstractStates.asIterable(locState).filter(ExpressionTreeReportingState.class)) {
        if (expressionTreeReportingState instanceof InvariantsState) {
          InvariantsState invState = (InvariantsState) expressionTreeReportingState;
          boolean skip = false;
          for (InvariantsState other : invStates) {
            if (invState.isLessOrEqual(other)) {
              skip = true;
              break;
            }
          }
          if (skip) {
            stateInvariant = ExpressionTrees.getFalse();
            continue;
          }
          invStates.add(invState);
        } else {
          otherReportingStates = true;
        }
        stateInvariant =
            And.of(
                stateInvariant,
                expressionTreeReportingState.getFormulaApproximation(
                    cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
      }

      locationInvariant = Or.of(locationInvariant, stateInvariant);
    }

    if (!otherReportingStates && invStates.size() > 1) {
      Set<InvariantsState> newInvStates = Sets.newHashSet();
      for (InvariantsState a : invStates) {
        boolean skip = false;
        for (InvariantsState b : invStates) {
          if (a != b && a.isLessOrEqual(b)) {
            skip = true;
            break;
          }
        }
        if (!skip) {
          newInvStates.add(a);
        }
      }
      if (newInvStates.size() < invStates.size()) {
        locationInvariant = ExpressionTrees.getFalse();
        for (InvariantsState state : newInvStates) {
          locationInvariant =
              Or.of(
                  locationInvariant,
                  state.getFormulaApproximation(
                      cfa.getFunctionHead(pLocation.getFunctionName()), pLocation));
        }
      }
    }

    return locationInvariant;
  }
}
