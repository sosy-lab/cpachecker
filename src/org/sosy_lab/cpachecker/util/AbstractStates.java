/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeTraverser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Helper class that provides several useful methods for handling AbstractStates.
 */
public final class AbstractStates {

  private AbstractStates() { }

  /**
   * Retrieve one of the wrapped abstract states by type. If the hierarchy of
   * (wrapped) abstract states has several levels, this method searches through
   * them recursively.
   *
   * The type does not need to match exactly, the returned state has just to
   * be a sub-type of the type passed as argument.
   *
   * If you want to get all wrapped states with this type,
   * use <code>asIterable(pState).filter(pType)</code>.
   *
   * @param <T> The type of the wrapped state.
   * @param pState An abstract state
   * @param pType The class object of the type of the wrapped state.
   * @return An instance of an state with type T or null if there is none.
   */
  public static <T extends AbstractState> T extractStateByType(AbstractState pState, Class<T> pType) {
    if (pType.isInstance(pState)) {
      return pType.cast(pState);

    } else if (pState instanceof AbstractSingleWrapperState) {
      AbstractState wrapped = ((AbstractSingleWrapperState)pState).getWrappedState();
      return extractStateByType(wrapped, pType);

    } else if (pState instanceof AbstractWrapperState) {
      for (AbstractState wrapped : ((AbstractWrapperState)pState).getWrappedStates()) {
        T result = extractStateByType(wrapped, pType);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  /**
   * Applies {@link #extractStateByType(AbstractState, Class)} to all states
   * of a given {@link Iterable}.
   * There is one state in the output for every state in the input
   * that has a wrapped state with a matching type, in the same order.
   * Input states without a matching wrapped state are silently ignored.
   *
   * If you want to get all wrapped states with the given type,
   * even if a single state in the input has several of them,
   * use <code>asFlatIterable(states).filter(pType)</code>.   *
   *
   * @param states an <code>Iterable</code> over all the states
   *        <code>extractStateByType(AbstractState, Class)</code>
   *        should be applied on
   * @param pType the type to use in each call of
   *        <code>extractStateByType(AbstractState, Class)</code>
   *
   * @return an <code>Iterable</code> over all the returned states
   *         without <code>null</code> values
   */
  public static <T extends AbstractState> FluentIterable<T> projectToType(Iterable<AbstractState> states, Class<T> pType) {
    return from(states).transform(toState(pType))
                        .filter(notNull());
  }

  public static CFANode extractLocation(AbstractState pState) {
    AbstractStateWithLocation e = extractStateByType(pState, AbstractStateWithLocation.class);
    return e == null ? null : e.getLocationNode();
  }

  public static Optional<CallstackStateEqualsWrapper> extractOptionalCallstackWraper(AbstractState pState) {
    CallstackState callstack = extractStateByType(pState, CallstackState.class);
    return callstack == null ? Optional.empty() : Optional.of(new CallstackStateEqualsWrapper(callstack));
  }

  public static Iterable<CFANode> extractLocations(AbstractState pState) {
    AbstractStateWithLocations e = extractStateByType(pState, AbstractStateWithLocations.class);
    return e == null ? ImmutableList.<CFANode>of() : e.getLocationNodes();
  }

  public static FluentIterable<CFANode> extractLocations(Iterable<AbstractState> pStates) {
    return from(pStates).transformAndConcat(AbstractStates::extractLocations);
  }

  public static Iterable<CFAEdge> getOutgoingEdges(AbstractState pState) {
    return extractStateByType(pState, AbstractStateWithLocation.class).getOutgoingEdges();
  }

  public static final Function<AbstractState, CFANode> EXTRACT_LOCATION =
      AbstractStates::extractLocation;

  public static Iterable<AbstractState> filterLocation(Iterable<AbstractState> pStates, CFANode pLoc) {
    if (pStates instanceof LocationMappedReachedSet) {
      // only do this for LocationMappedReachedSet, not for all ReachedSet,
      // because this method is imprecise for the rest
      return ((LocationMappedReachedSet)pStates).getReached(pLoc);
    }

    Predicate<AbstractState> statesWithRightLocation =
        Predicates.compose(equalTo(pLoc), AbstractStates::extractLocation);
    return FluentIterable.from(pStates).filter(statesWithRightLocation);
  }

  public static FluentIterable<AbstractState> filterLocations(Iterable<AbstractState> pStates, Set<CFANode> pLocs) {
    if (pStates instanceof LocationMappedReachedSet) {
      // only do this for LocationMappedReachedSet, not for all ReachedSet,
      // because this method is imprecise for the rest
      final LocationMappedReachedSet states = (LocationMappedReachedSet)pStates;
      return from(pLocs).transformAndConcat(states::getReached);
    }

    Predicate<AbstractState> statesWithRightLocation =
        Predicates.compose(in(pLocs), AbstractStates::extractLocation);
    return from(pStates).filter(statesWithRightLocation);
  }

  public static boolean isTargetState(AbstractState as) {
    return (as instanceof Targetable) && ((Targetable)as).isTarget();
  }

  public static final Predicate<AbstractState> IS_TARGET_STATE = AbstractStates::isTargetState;

  public static boolean hasAssumptions(AbstractState as) {
    AssumptionStorageState assumption = extractStateByType(as, AssumptionStorageState.class);
    return assumption != null && !assumption.isStopFormulaTrue() && !assumption.isAssumptionTrue();
  }

  /**
   * Returns a {@link Function} object for {@link #extractStateByType(AbstractState, Class)}.
   *
   * @param pType the type to use in the call of
   *        <code>extractStateByType(AbstractState, Class)</code> for parameter
   *        <code>Class</code>
   *
   * @return a <code>Function</code> for
   *        <code>extractStateByType(AbstractState, Class)</code> using the given
   *        type
   */
  public static <T extends AbstractState>
                Function<AbstractState, T> toState(final Class<T> pType) {

    return as -> extractStateByType(as, pType);
  }

  /**
   * Creates a {@link FluentIterable} that enumerates all the <code>AbstractStates</code>
   * contained in a given state pre-order. The root state itself is included, the states
   * are unwrapped recursively.
   *
   * <p><b>Example</b>: State A wraps states B and C. State B wraps states D and E.<br />
   *             The resulting tree (see below) is traversed pre-order.
   * <pre>
   *                  A
   *                 / \
   *                B   C
   *               / \
   *              D   E
   * </pre>
   * The returned <code>FluentIterable</code> iterates over the items in the following
   * order : A, B, D, E, C.
   * </p>
   *
   * @param as the root state
   *
   * @return a <code>FluentIterable</code> over the given root state and all states
   *         that are wrapped in it, recursively
   */
  public static FluentIterable<AbstractState> asIterable(final AbstractState as) {

    return new TreeTraverser<AbstractState>() {

      @Override
      public Iterable<AbstractState> children(AbstractState state) {
        if (state instanceof AbstractSingleWrapperState) {
          AbstractState wrapped = ((AbstractSingleWrapperState)state).getWrappedState();
          return ImmutableList.of(wrapped);

        } else if (state instanceof AbstractWrapperState) {
          return ((AbstractWrapperState)state).getWrappedStates();
        }

        return ImmutableList.of();
      }
    }.preOrderTraversal(as);
  }

  /**
   * Apply {@link #asIterable(AbstractState)} to several abstract states at once
   * and provide an iterable for all resulting component abstract states.
   * There is no distinction from which state in the input iterable
   * a state in the output iterable results,
   * and there is no guaranteed order.
   */
  public static FluentIterable<AbstractState> asFlatIterable(final Iterable<AbstractState> pStates) {
    return from(pStates).transformAndConcat(AbstractStates::asIterable);
  }

  /**
   * Returns a predicate representing states represented by
   * the given abstract state, according to reported
   * formulas
   */
  public static BooleanFormula extractReportedFormulas(FormulaManagerView manager, AbstractState state) {
    List<BooleanFormula> result = new ArrayList<>();

    // traverse through all the sub-states contained in this state
    for (FormulaReportingState s : asIterable(state).filter(FormulaReportingState.class)) {

      result.add(s.getFormulaApproximation(manager));
    }

    return manager.getBooleanFormulaManager().and(result);
  }
}
