// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

public class ReachedSetBasedExpressionTreeSupplier implements ExpressionTreeSupplier {

  private final LazyLocationMapping lazyLocationMapping;
  private final CFA cfa;

  public ReachedSetBasedExpressionTreeSupplier(LazyLocationMapping pLazyLocationMapping, CFA pCFA) {
    lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    cfa = Objects.requireNonNull(pCFA);
  }

  @Override
  public ExpressionTree<Object> getInvariantFor(CFANode pLocation) throws InterruptedException {

    Set<InvariantsState> invStates = new HashSet<>();
    boolean otherReportingStates = false;
    Iterable<AbstractState> locationStates = lazyLocationMapping.get(pLocation, Optional.empty());
    if (Iterables.isEmpty(locationStates)) {
      // Location is not necessarily unreachable, but might have been skipped,
      // e.g. by the multi-edge/basic-block optimization of the CompositeCPA.
      // In this case, we err on the side of caution.
      return ExpressionTrees.getTrue();
    }

    List<ExpressionTree<Object>> locationInvariants = new ArrayList<>();
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
      locationInvariants.add(stateInvariant);
    }
    ExpressionTree<Object> locationInvariant = Or.of(locationInvariants);

    if (!otherReportingStates && invStates.size() > 1) {
      Set<InvariantsState> newInvStates = new HashSet<>();
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
