/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * Helper class that provides several useful methods for handling AbstractElements.
 */
public final class AbstractElements {

  private AbstractElements() { }

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
  public static <T extends AbstractElement> T extractElementByType(AbstractElement pElement, Class<T> pType) {
    if (pType.isInstance(pElement)) {
      return pType.cast(pElement);

    } else if (pElement instanceof AbstractSingleWrapperElement) {
      AbstractElement wrapped = ((AbstractSingleWrapperElement)pElement).getWrappedElement();
      return extractElementByType(wrapped, pType);

    } else if (pElement instanceof AbstractWrapperElement) {
      for (AbstractElement wrapped : ((AbstractWrapperElement)pElement).getWrappedElements()) {
        T result = extractElementByType(wrapped, pType);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  public static CFANode extractLocation(AbstractElement pElement) {
    AbstractElementWithLocation e = extractElementByType(pElement, AbstractElementWithLocation.class);
    return e == null ? null : e.getLocationNode();
  }

  public static final Function<AbstractElement, CFANode> EXTRACT_LOCATION = new Function<AbstractElement, CFANode>() {
    @Override
    public CFANode apply(AbstractElement pArg0) {
      return extractLocation(pArg0);
    }
  };

  public static Iterable<CFANode> extractLocations(Iterable<? extends AbstractElement> pElements) {
    if (pElements instanceof LocationMappedReachedSet) {
      return ((LocationMappedReachedSet)pElements).getLocations();
    }

    return filter(transform(pElements, EXTRACT_LOCATION),
                  Predicates.notNull());
  }

  public static Iterable<AbstractElement> filterLocation(Iterable<AbstractElement> pElements, CFANode pLoc) {
    if (pElements instanceof LocationMappedReachedSet) {
      // only do this for LocationMappedReachedSet, not for all ReachedSet,
      // because this method is imprecise for the rest
      return ((LocationMappedReachedSet)pElements).getReached(pLoc);
    }

    return filter(pElements, Predicates.compose(Predicates.equalTo(pLoc),
                                                EXTRACT_LOCATION));
  }

  public static boolean isTargetElement(AbstractElement e) {
    return (e instanceof Targetable) && ((Targetable)e).isTarget();
  }

  public static final Predicate<AbstractElement> IS_TARGET_ELEMENT = new Predicate<AbstractElement>() {
    @Override
    public boolean apply(AbstractElement pArg0) {
      return isTargetElement(pArg0);
    }
  };

  public static <T extends AbstractElement> Iterable<T> filterTargetElements(Iterable<T> pElements) {
    return filter(pElements, IS_TARGET_ELEMENT);
  }

  /**
   * Function object for {@link #extractElementByType(AbstractElement, Class)}.
   */
  public static <T extends AbstractElement>
                Function<AbstractElement, T> extractElementByTypeFunction(final Class<T> pType) {

    return new Function<AbstractElement, T>() {
      @Override
      public T apply(AbstractElement ae) {
        return extractElementByType(ae, pType);
      }
    };
  }

  /**
   * Creates an iterable that enumerates all the AbstractElements contained in
   * a single element, including the root element itself.
   * The tree of elements is traversed in pre-order.
   */
  public static Iterable<AbstractElement> asIterable(final AbstractElement ae) {

    return new Iterable<AbstractElement>() {
      @Override
      public Iterator<AbstractElement> iterator() {

        return new Iterator<AbstractElement>() {

          private final Deque<Iterator<? extends AbstractElement>> stack = new ArrayDeque<Iterator<? extends AbstractElement>>();
          {
            stack.push(Iterators.singletonIterator(ae));
          }

          @Override
          public boolean hasNext() {
            return !stack.isEmpty();
          }

          @Override
          public AbstractElement next() {
            Iterator<? extends AbstractElement> currentIterator = stack.peek();
            Preconditions.checkState(currentIterator.hasNext());
            AbstractElement current = currentIterator.next();

            if (!currentIterator.hasNext()) {
              stack.pop();
            }

            if (current instanceof AbstractSingleWrapperElement) {
              AbstractElement wrapped = ((AbstractSingleWrapperElement)current).getWrappedElement();
              stack.push(Iterators.singletonIterator(wrapped));

            } else if (current instanceof AbstractWrapperElement) {
              stack.push(((AbstractWrapperElement)current).getWrappedElements().iterator());
            }

            return current;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  private static final Function<AbstractElement, Iterable<AbstractElement>> AS_ITERABLE
    = new Function<AbstractElement, Iterable<AbstractElement>>() {
      @Override
      public Iterable<AbstractElement> apply(AbstractElement pElement) {
        return asIterable(pElement);
      }
    };

  public static Iterable<AbstractElement> asIterable(final Iterable<AbstractElement> pElements) {
    return Iterables.concat(transform(pElements, AS_ITERABLE));
  }
}