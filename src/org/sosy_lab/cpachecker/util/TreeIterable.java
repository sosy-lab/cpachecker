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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * A class that provides a convenient way to recursively iterate through a tree
 * of objects, given only a function that returns the children of a node.
 * The tree is traversed in pre-order.
 * @param <T> The type of the nodes of the tree.
 */
public class TreeIterable<T> implements Iterable<T> {

  private final Function<T, Iterator<? extends T>> childrenFunction;

  private final T root;

  public TreeIterable(T pRoot, Function<T, Iterator<? extends T>> pChildrenFunction) {
    root = pRoot;
    childrenFunction = pChildrenFunction;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      private final Deque<Iterator<? extends T>> stack = new ArrayDeque<Iterator<? extends T>>();
      {
        stack.push(Iterators.singletonIterator(root));
      }

      @Override
      public boolean hasNext() {
        return !stack.isEmpty();
      }

      @Override
      public T next() {
        Iterator<? extends T> currentIterator = stack.peek();
        Preconditions.checkState(currentIterator.hasNext());
        T current = currentIterator.next();

        if (!currentIterator.hasNext()) {
          stack.pop();
        }

        Iterator<? extends T> children = childrenFunction.apply(current);
        if (children != null && children.hasNext()) {
          stack.push(children);
        }

        return current;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}