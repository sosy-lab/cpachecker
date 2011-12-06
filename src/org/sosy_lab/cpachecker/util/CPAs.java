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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * Helper functions to work with CPAs.
 */
public class CPAs {

  private CPAs() { }

  /**
   * Retrieve a specific CPA out of a structure of wrapper and composite CPAs.
   *
   * @param cpa The root of the tree of CPAs where to search.
   * @param cls The type to search for.
   * @return The found CPA, or null if none was found.
   */
  public <T extends ConfigurableProgramAnalysis> T retrieveCPA(ConfigurableProgramAnalysis cpa, Class<T> cls) {
    if (cls.isInstance(cpa)) {
      return cls.cast(cpa);
    } else if (cpa instanceof WrapperCPA) {
      return ((WrapperCPA)cpa).retrieveWrappedCpa(cls);
    } else {
      return null;
    }
  }

  /**
   * Creates an iterable that enumerates all the CPAs contained in
   * a single CPA, including the root CPA itself.
   * The tree of elements is traversed in pre-order.
   */
  public static Iterable<ConfigurableProgramAnalysis> asIterable(final ConfigurableProgramAnalysis pCpa) {

    return new Iterable<ConfigurableProgramAnalysis>() {
      @Override
      public Iterator<ConfigurableProgramAnalysis> iterator() {

        return new Iterator<ConfigurableProgramAnalysis>() {

          private final Deque<Iterator<? extends ConfigurableProgramAnalysis>> stack = new ArrayDeque<Iterator<? extends ConfigurableProgramAnalysis>>();
          {
            stack.push(Iterators.singletonIterator(pCpa));
          }

          @Override
          public boolean hasNext() {
            return !stack.isEmpty();
          }

          @Override
          public ConfigurableProgramAnalysis next() {
            Iterator<? extends ConfigurableProgramAnalysis> currentIterator = stack.peek();
            Preconditions.checkState(currentIterator.hasNext());
            ConfigurableProgramAnalysis current = currentIterator.next();

            if (!currentIterator.hasNext()) {
              stack.pop();
            }

            if (current instanceof WrapperCPA) {
              stack.push(((WrapperCPA)current).getWrappedCPAs().iterator());
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
