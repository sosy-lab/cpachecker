/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.collect.Iterables.*;

import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * Helper class that provides several useful methods for handling AbstractStates.
 */
public final class AbstractStates {

  private AbstractStates() { }

  /**
   * Retrieve one of the wrapped abstract elements by type. If the hierarchy of
   * (wrapped) abstract elements has several levels, this method searches through
   * them recursively.
   *
   * The type does not need to match exactly, the returned element has just to
   * be a sub-type of the type passed as argument.
   *
   * @param <T> The type of the wrapped element.
   * @param An abstract element
   * @param pType The class object of the type of the wrapped element.
   * @return An instance of an element with type T or null if there is none.
   */
  public static <T extends AbstractState> T extractStateByType(AbstractState pElement, Class<T> pType) {
    if (pType.isInstance(pElement)) {
      return pType.cast(pElement);

    } else if (pElement instanceof AbstractSingleWrapperState) {
      AbstractState wrapped = ((AbstractSingleWrapperState)pElement).getWrappedState();
      return extractStateByType(wrapped, pType);

    } else if (pElement instanceof AbstractWrapperState) {
      for (AbstractState wrapped : ((AbstractWrapperState)pElement).getWrappedStates()) {
        T result = extractStateByType(wrapped, pType);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  /**
   * Apply {@link #extractStateByType(AbstractState, Class)} to all elements
   * of an Iterable.
   * The returned Iterable does not contain nulls.
   */
  public static <T extends AbstractState> Iterable<T> projectToType(Iterable<AbstractState> elements, Class<T> pType) {
    return Iterables.filter(
              Iterables.transform(elements, extractStateByTypeFunction(pType)),
              Predicates.notNull());
  }

  /**
   * Retrieve all wrapped elements of a certain type, if there are any of them.
   *
   * The type does not need to match exactly, the returned elements have just to
   * be of sub-types of the type passed as argument.
   *
   * The returned Iterable contains the elements in pre-order.
   */
  public static <T extends AbstractState> Iterable<T> extractAllStatesOfType(AbstractState pElement, Class<T> pType) {
    return Iterables.filter(asIterable(pElement), pType);
  }

  public static CFANode extractLocation(AbstractState pElement) {
    AbstractStateWithLocation e = extractStateByType(pElement, AbstractStateWithLocation.class);
    return e == null ? null : e.getLocationNode();
  }

  public static final Function<AbstractState, CFANode> EXTRACT_LOCATION = new Function<AbstractState, CFANode>() {
    @Override
    public CFANode apply(AbstractState pArg0) {
      return extractLocation(pArg0);
    }
  };

  public static Iterable<CFANode> extractLocations(Iterable<? extends AbstractState> pElements) {
    if (pElements instanceof LocationMappedReachedSet) {
      return ((LocationMappedReachedSet)pElements).getLocations();
    }

    return filter(transform(pElements, EXTRACT_LOCATION),
                  Predicates.notNull());
  }

  public static Iterable<AbstractState> filterLocation(Iterable<AbstractState> pElements, CFANode pLoc) {
    if (pElements instanceof LocationMappedReachedSet) {
      // only do this for LocationMappedReachedSet, not for all ReachedSet,
      // because this method is imprecise for the rest
      return ((LocationMappedReachedSet)pElements).getReached(pLoc);
    }

    return filter(pElements, Predicates.compose(Predicates.equalTo(pLoc),
                                                EXTRACT_LOCATION));
  }

  public static boolean isTargetState(AbstractState e) {
    return (e instanceof Targetable) && ((Targetable)e).isTarget();
  }

  public static final Predicate<AbstractState> IS_TARGET_STATE = new Predicate<AbstractState>() {
    @Override
    public boolean apply(AbstractState pArg0) {
      return isTargetState(pArg0);
    }
  };

  public static <T extends AbstractState> Iterable<T> filterTargetStates(Iterable<T> pElements) {
    return filter(pElements, IS_TARGET_STATE);
  }

  /**
   * Function object for {@link #extractStateByType(AbstractState, Class)}.
   */
  public static <T extends AbstractState>
                Function<AbstractState, T> extractStateByTypeFunction(final Class<T> pType) {

    return new Function<AbstractState, T>() {
      @Override
      public T apply(AbstractState ae) {
        return extractStateByType(ae, pType);
      }
    };
  }

  /**
   * Creates an iterable that enumerates all the AbstractStates contained in
   * a single element, including the root element itself.
   * The tree of elements is traversed in pre-order.
   */
  public static Iterable<AbstractState> asIterable(final AbstractState ae) {

    return new TreeIterable<AbstractState>(ae, ABSTRACT_STATE_CHILDREN_FUNCTION);
  }

  private static final Function<AbstractState, Iterator<? extends AbstractState>> ABSTRACT_STATE_CHILDREN_FUNCTION
    = new Function<AbstractState, Iterator<? extends AbstractState>>() {
      @Override
      public Iterator<? extends AbstractState> apply(AbstractState element) {
        if (element instanceof AbstractSingleWrapperState) {
          AbstractState wrapped = ((AbstractSingleWrapperState)element).getWrappedState();
          return Iterators.singletonIterator(wrapped);

        } else if (element instanceof AbstractWrapperState) {
          return ((AbstractWrapperState)element).getWrappedStates().iterator();
        }

        return Iterators.emptyIterator();
      }
    };

  private static final Function<AbstractState, Iterable<AbstractState>> AS_ITERABLE
    = new Function<AbstractState, Iterable<AbstractState>>() {
      @Override
      public Iterable<AbstractState> apply(AbstractState pElement) {
        return asIterable(pElement);
      }
    };

  public static Iterable<AbstractState> asIterable(final Iterable<AbstractState> pElements) {
    return Iterables.concat(transform(pElements, AS_ITERABLE));
  }

  /**
   * Returns a predicate representing states represented by
   * the given abstract element, according to reported
   * formulas
   */
  public static Formula extractReportedFormulas(FormulaManager manager, AbstractState element) {
    Formula result = manager.makeTrue();

    // traverse through all the sub-elements contained in this element
    for (FormulaReportingState e :  extractAllStatesOfType(element, FormulaReportingState.class)) {

      result = manager.makeAnd(result, e.getFormulaApproximation(manager));
    }

    return result;
  }
}
