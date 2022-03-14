// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * This class contains helper code that is needed for dealing with the invariants, but needs access
 * to (package) private classes in the automaton package.
 */
public enum AutomatonInvariantsUtils {
  ;

  /**
   * @throws CPAException if states with invariants were found that are in the specification
   *     automata, but not in the supplied {@link UnmodifiableReachedSet}
   */
  public static void checkForMissedInvariants(
      Specification pAutomataAsSpec, UnmodifiableReachedSet pReachedSet) throws CPAException {
    final Set<AutomatonInternalState> automatonStatesWithInvariants = new LinkedHashSet<>();

    // fill automatonStatesWithInvariants with all internal automaton states that we find in the
    // automata:
    for (Automaton aut : pAutomataAsSpec.getSpecificationAutomata()) {
      aut.getStates().stream()
          .map(AutomatonInternalState::getTransitions)
          .flatMap(Collection::stream)
          .forEach(
              x -> {
                ExpressionTree<AExpression> exprTree = x.getCandidateInvariants();
                if (!exprTree.equals(ExpressionTrees.getTrue())) {
                  automatonStatesWithInvariants.add(x.getFollowState());
                }
              });
    }

    // remove all internal automaton states in case they are represented in the reached set:
    for (AbstractState abstractState : pReachedSet) {
      for (AutomatonState automatonState :
          AbstractStates.asIterable(abstractState).filter(AutomatonState.class)) {
        automatonStatesWithInvariants.remove(automatonState.getInternalState());
      }
    }

    // now the set contains all states that are unaccounted for:
    if (!automatonStatesWithInvariants.isEmpty()) {
      throw new CPAException(
          String.format(
              "There are unaccounted invariants in the witness(es)!(Invariants of the following"
                  + " states: %s)",
              automatonStatesWithInvariants.toString()));
    }
  }
}
