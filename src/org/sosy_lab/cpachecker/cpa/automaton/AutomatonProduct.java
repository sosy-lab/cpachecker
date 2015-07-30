/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;


public final class AutomatonProduct {

  /**
   * Product of a set of automata states.
   */
  public static class ProductState {

    private final ImmutableList<AutomatonInternalState> componentStates;
    private final Multimap<AutomatonTransition, ProductState> transitions;

    public ProductState(List<AutomatonInternalState> pComponents) {
      this.componentStates = ImmutableList.copyOf(pComponents);
      this.transitions = LinkedListMultimap.create();
    }

    public ImmutableList<AutomatonInternalState> getComponents() {
      return componentStates;
    }

    public static ProductState of(List<AutomatonInternalState> pComponents) {
      return new ProductState(pComponents);
    }

    public void addTransition(AutomatonTransition pT, ProductState pTarget) {
      transitions.put(pT, pTarget);
    }

    public Multimap<AutomatonTransition, ProductState> getTransitions() {
      return transitions;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((componentStates == null) ? 0 : componentStates.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ProductState other = (ProductState) obj;
      if (componentStates == null) {
        if (other.componentStates != null) {
          return false;
        }
      } else if (!componentStates.equals(other.componentStates)) {
        return false;
      }
      return true;
    }

  }

  /**
   * Product of two control automata.
   *
   * @param pA1   automaton 1
   * @param pA2   automaton 2
   *
   * @return  product
   */
  public static Automaton productOf(Automaton pA1, Automaton pA2) {
    return productOf(Lists.newArrayList(pA1, pA2));
  }

  /**
   * Product of a collection of control automata.
   *
   * @param pAutomata   collection of control automata
   *
   * @return  product
   */
  public static Automaton productOf(List<Automaton> pAutomata) {

    Set<ProductState> productStates = Sets.newHashSet();

    // Compute the initial state of the product automaton
    //
    final ProductState initialState = ProductState.of(Lists.transform(pAutomata,
        new Function<Automaton, AutomatonInternalState>() {
          @Override
          public AutomatonInternalState apply(Automaton pA) {
            return pA.getInitialState();
          }
        }));

    productStates.add(initialState);


    // For a given product state:
    //  For all transitions:
    //    For all automata:
    //      what would be the successor state?
    //
    //  TODO: How should we handle "MATCH ALL" vs "MATCH FIRST"?
    //    FIRST STEP: IGNORE MATCH ALL!!!
    //
    Deque<ProductState> worklist = Queues.newArrayDeque();
    worklist.add(initialState);

    while(!worklist.isEmpty()) {
      final ProductState current = worklist.pop();

      // The order of the transitions might be important!!!!
      final LinkedHashSet<AutomatonTransition> transitions;
      transitions = getUniqueOutgoingTransitions(current);

      for (AutomatonTransition t: transitions) {
        List<AutomatonInternalState> successorComponents = Lists.newArrayListWithExpectedSize(current.getComponents().size());

        // For t: Get the successor state of each component automaton state
        for (AutomatonInternalState a: current.getComponents()) {
          assert !a.getDoesMatchAll();

          // if contained: proceed to its successor, otherwise: stay in the same state
          final AutomatonInternalState succ;
          if (containsEqualTransition(t, a.getTransitions())) {
            succ = t.getFollowState();
          } else {
            succ = a;
          }

          successorComponents.add(succ);
        }

        // Build the successor state from its components
        ProductState next = ProductState.of(successorComponents);
        if (productStates.add(next)) {
          //
          // Add it to the work list if it has not already been handled
          worklist.add(next);
        }

        // Add the transition 't' to 'next' to the 'current' state!
        current.addTransition(t, next);
      }
    }

    return createAutomaton(initialState, productStates);
  }

  private static Automaton createAutomaton(ProductState pInitialState, Set<ProductState> pProductStates) {
    return null;
  }

  private static boolean containsEqualTransition(AutomatonTransition pT, Collection<AutomatonTransition> pC) {
    return false;
  }

  private static LinkedHashSet<AutomatonTransition> getUniqueOutgoingTransitions(ProductState pCurrent) {
    return null;

  }
}
