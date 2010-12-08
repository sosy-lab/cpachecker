/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Helper class that provides several useful methods for handling AbstractElements.
 */
public final class AbstractElements {

  private AbstractElements() { }
  
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
  
  public static boolean isTargetElement(AbstractElement e) {
    return (e instanceof Targetable) && ((Targetable)e).isTarget();
  }
  
  public static Predicate<AbstractElement> FILTER_TARGET_ELEMENTS = new Predicate<AbstractElement>() {
    @Override
    public boolean apply(AbstractElement pArg0) {
      return isTargetElement(pArg0);
    }
  };
  
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
}
