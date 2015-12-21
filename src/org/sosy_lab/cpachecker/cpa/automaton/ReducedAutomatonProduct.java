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
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.TrinaryEqualable.Equality;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition.PlainAutomatonTransition;
import org.sosy_lab.cpachecker.util.Cartesian;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;


public final class ReducedAutomatonProduct {

  public static enum TransitionCollectionQuality {
    DISTINCT, // each transition matches for different cases
    UNKNOWN   // the choice might be ambiguous
  }

  public static enum Trinary {
    TRUE,
    FALSE,
    UNKNOWN;
  }

  public final static class ReducedTransitionList {

    private TransitionCollectionQuality quality = TransitionCollectionQuality.DISTINCT;
    private Map<PlainAutomatonTransition, Collection<AutomatonInternalState>> transitions = Maps.newIdentityHashMap();

    private ReducedTransitionList() {
    }

    public TransitionCollectionQuality getQuality() {
      return quality;
    }

    public ImmutableMap<PlainAutomatonTransition, Collection<AutomatonInternalState>> getTransitions() {
      return ImmutableMap.copyOf(transitions);
    }

    public static ReducedTransitionList from(ProductState pFromState) {
      final ReducedTransitionList result = new ReducedTransitionList();

      for (AutomatonInternalState q: pFromState.getComponents()) {
        for (AutomatonTransition t: q.getTransitions()) {
          // Check if there is already an equivalent transition!
          PlainAutomatonTransition pat = PlainAutomatonTransition.deriveFrom(t);
          PlainAutomatonTransition equalPatFound = null;

          for (PlainAutomatonTransition et: result.transitions.keySet()) {
            Equality eq = t.isEquivalentTo(et);
            if (eq == Equality.EQUAL) {
              equalPatFound = et;
              break;
            } else if (eq == Equality.UNKNOWN) {
              result.quality = TransitionCollectionQuality.UNKNOWN;
            }
          }

          if (equalPatFound == null) {
            result.transitions.put(pat, Collections.singleton(t.getFollowState()));
          } else {
            List<AutomatonInternalState> successors = Lists.newArrayList(result.transitions.get(equalPatFound));
            successors.add(q);
            result.transitions.put(equalPatFound, successors);
          }
        }
      }

      return result;
    }
  }

  /**
   * Product of a set of automata states.
   */
  final static class ProductState {

    private final ImmutableList<AutomatonInternalState> componentStates;
    private transient final Multimap<PlainAutomatonTransition, ProductState> transitions;
    private boolean isAcceptingState;

    public ProductState(List<AutomatonInternalState> pComponents) {
      this.componentStates = ImmutableList.copyOf(pComponents);
      this.transitions = LinkedHashMultimap.create();

      this.isAcceptingState = false;
      for (AutomatonInternalState q: pComponents) {
        if (q.isTarget()) {
          this.isAcceptingState = true;
        }
      }
    }

    public ImmutableList<AutomatonInternalState> getComponents() {
      return componentStates;
    }

    public static Iterable<ProductState> of(List<List<AutomatonInternalState>> pComponents) {

      LinkedList<ProductState> result = Lists.newLinkedList();
      Iterable<List<AutomatonInternalState>> x = Cartesian.product(pComponents);

      for (List<AutomatonInternalState> componentStates: x) {
        result.add(new ProductState(componentStates));
      }

      return result;
    }

    public void addTransition(PlainAutomatonTransition pT, ProductState pTarget) {
      transitions.put(pT, pTarget);
    }

    public Multimap<PlainAutomatonTransition, ProductState> getTransitions() {
      return transitions;
    }

    public String getStateName() {
      StringBuilder result = new StringBuilder();
      for (AutomatonInternalState q: componentStates) {
        if (result.length() > 0) {
          result.append("/");
        }
        result.append(q.getName());
      }
      return result.toString();
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
      } else if (!Objects.deepEquals(componentStates, componentStates)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return componentStates.hashCode();
    }
  }

  /**
   * Product of two control automata.
   *
   * @param pA1   automaton 1
   * @param pA2   automaton 2
   *
   * @return  product
   * @throws InvalidAutomatonException
   */
  public static Automaton productOf(Automaton pA1, Automaton pA2, String pProductAutomataName) throws InvalidAutomatonException {
    return productOf(Lists.newArrayList(pA1, pA2), pProductAutomataName);
  }

  /**
   * Product of a collection of control automata.
   *
   * @param pAutomata   collection of control automata
   *
   * @return  product
   * @throws InvalidAutomatonException
   */
  public static Automaton productOf(List<Automaton> pAutomata, String pProductAutomataName) throws InvalidAutomatonException {

    Set<ProductState> productStates = Sets.newHashSet();

    // Compute the initial state of the product automaton
    //
    final ProductState initialState = ProductState.of(Lists.transform(pAutomata,
        new Function<Automaton, List<AutomatonInternalState>>() {
          @Override
          public List<AutomatonInternalState> apply(Automaton pA) {
            return ImmutableList.of(pA.getInitialState());
          }
        })).iterator().next();

    productStates.add(initialState);


    // For a given product state:
    //  For all transitions:
    //    For all automata:
    //      what would be the successor state?
    //
    //  TODO: How should we handle "MATCH ALL" vs "MATCH FIRST"?
    //    FIRST STEP: IGNORE MATCH ALL!!!
    //
    // TODO: Implement a checker for checking whether two transition have an intersection in the matched edges
    //
    Deque<ProductState> worklist = Queues.newArrayDeque();
    worklist.add(initialState);

    while(!worklist.isEmpty()) {
      // FOR the product state 'current':
      final ProductState current = worklist.pop();

      // -- The order of the transitions might be important!!!!
      final ReducedTransitionList transitions = ReducedTransitionList.from(current);
      ImmutableMap<PlainAutomatonTransition, Collection<AutomatonInternalState>> tx = transitions.getTransitions();

      // FOR ALL transitions 't' outgoing from 'current'...
      for (PlainAutomatonTransition t: tx.keySet()) {

        // -- List for the components of the new (product) successor state
        List<List<AutomatonInternalState>> successorComponents = Lists.newArrayListWithExpectedSize(current.getComponents().size());

        // FOR 't': Get the successor state of each component automaton state
        for (AutomatonInternalState a: current.getComponents()) {
          // What other states are reachable using the same transition?

          // if contained: proceed to its successor, otherwise: stay in the same state
          final List<AutomatonInternalState> succ;

          // Perform the transition only if transitions of other automata are (fore sure) EQUAL!!!
          List<AutomatonInternalState> to = equalTransitionsTo(t, a.getTransitions());
          if (to.isEmpty()) {
            // Stay for this transition in the same state.
            succ = ImmutableList.of(a);
          } else {
            succ = to;
          }

          successorComponents.add(succ);
        }

        // Build the successor states from its components
        final Iterable<ProductState> next = ProductState.of(successorComponents);

        for (ProductState n: next) {
          if (productStates.add(n)) {
            //
            // Add it to the work list if it has not already been handled
            if (!isBottom(n)) {
              worklist.add(n);
            }
          }

          // Add the transition 't' to 'next' to the 'current' state!
          current.addTransition(t, n);
        }
      }
    }

    return createAutomaton(initialState, productStates, pProductAutomataName);
  }

  private static boolean isBottom(ProductState pN) {
    for (AutomatonInternalState a: pN.getComponents()) {
      if (a.equals(AutomatonInternalState.BOTTOM)) {
        return true;
      }
    }
    return false;
  }

  private static Automaton createAutomaton(
      ProductState pInitialState, Set<ProductState> pProductStates, String pProductAutomataName)
          throws InvalidAutomatonException {

    final String initialStateName = pInitialState.getStateName();

    List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    for (ProductState ps: pProductStates) {
      // Create the transitions
      List<AutomatonTransition> transitions = Lists.newArrayList();
      for (PlainAutomatonTransition t : ps.getTransitions().keySet()) {
        Collection<ProductState> successors = ps.getTransitions().get(t);
        for (ProductState succ: successors) {
          transitions.add(new AutomatonTransition(
              t.trigger,
              Collections.singletonList(t.assertion),
              t.assumption,
              t.assumptionTruth,
              t.shadowCode,
              t.actions,
              succ.getStateName(),
              null,
              t.violatedWhenEnteringTarget,
              t.violatedWhenAssertionFailed));
        }
      }

      // Create and add the state
      automatonStates.add(new AutomatonInternalState(
          ps.getStateName(), transitions, ps.isAcceptingState, true));

      // TODO: split needed before target states!
      //    The split might (should?) already be part of the input automata!!
    }

    return new Automaton(null, pProductAutomataName,
        Maps.<String, AutomatonVariable>newHashMap(),
        automatonStates, initialStateName);
  }

  private static List<AutomatonInternalState> equalTransitionsTo(PlainAutomatonTransition pEqualTo, Collection<AutomatonTransition> pT) {
    List<AutomatonInternalState> result = Lists.newArrayList();
    for (AutomatonTransition t: pT) {
      if (t.isEquivalentTo(pEqualTo) == Equality.EQUAL) {
        result.add(t.getFollowState());
      }
    }
    return result;
  }

}
