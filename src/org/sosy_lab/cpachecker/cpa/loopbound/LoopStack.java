/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

final class LoopStack implements Iterable<LoopIterationState> {

  private static final LoopStack EMPTY_STACK = new LoopStack();

  private final LoopIterationState head;

  private final LoopStack tail;

  private final int size;

  private int hashCache = 0;

  private LoopStack() {
    head = null;
    tail = null;
    size = 0;
  }

  LoopStack(LoopIterationState pLoop) {
    head = Objects.requireNonNull(pLoop);
    tail = EMPTY_STACK;
    size = 1;
  }

  private LoopStack(LoopIterationState pHead, LoopStack pTail) {
    head = Objects.requireNonNull(pHead);
    tail = pTail;
    size = pTail.size + 1;
  }

  public LoopIterationState peek() {
    if (isEmpty()) {
      throw new NoSuchElementException("Stack is empty.");
    }
    return head;
  }

  public LoopStack pop() {
    checkState(!isEmpty(), "Stack is empty.");
    return tail;
  }

  public LoopStack push(LoopIterationState pHead) {
    return new LoopStack(pHead, this);
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public int getSize() {
    return size;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "";
    }
    if (tail.isEmpty()) {
      return head.toString();
    }
    return String.format("%s (%s)", head, tail);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof LoopStack) {
      LoopStack other = (LoopStack) pObj;
      // Hash code is cached, so this is also quick
      if (hashCode() != other.hashCode()) {
        return false;
      }
      return size == other.size
          && Objects.equals(head, other.head)
          && Objects.equals(tail, other.tail);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      // No need to hash size; it is already implied by tail
      hashCache = Objects.hash(head, tail);
    }
    return hashCache;
  }

  @Override
  public Iterator<LoopIterationState> iterator() {
    return new Iterator<>() {

      private LoopStack current = LoopStack.this;

      @Override
      public boolean hasNext() {
        return !current.isEmpty();
      }

      @Override
      public LoopIterationState next() {
        LoopIterationState next = current.peek();
        current = current.pop();
        return next;
      }
    };
  }

}