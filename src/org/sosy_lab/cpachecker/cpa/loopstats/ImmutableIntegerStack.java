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
package org.sosy_lab.cpachecker.cpa.loopstats;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

public class ImmutableIntegerStack implements ImmutableStack<Integer> {

  @Nullable private final ImmutableIntegerStack tail;
  @Nullable private final Integer head;
  private final int size;

  private static ImmutableIntegerStack EMPTYSTACK = new ImmutableIntegerStack();

  private ImmutableIntegerStack(final Integer pHead, final ImmutableIntegerStack pTail) {
    head = Preconditions.checkNotNull(pHead);
    tail = Preconditions.checkNotNull(pTail);
    size = pTail.size() + 1;
  }

  private ImmutableIntegerStack() {
    head = null;
    tail = null;
    size = 0;
  }

  public static ImmutableIntegerStack singleton(Integer pElement) {
    return new ImmutableIntegerStack(pElement, EMPTYSTACK);
  }

  public static ImmutableIntegerStack empty() {
    return EMPTYSTACK;
  }

  @Override
  public ImmutableIntegerStack getTail() {
    return tail;
  }

  @Override
  public Integer peekHead() {
    return head;
  }

  @Override
  public boolean isEmpty() {
    return head == null;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return String.format("Stack is empty!", size());
    } else {
      return String.format("head: %d; size: %d.", peekHead(), size());
    }
  }

  @Override
  public ImmutableIntegerStack push(Integer pNewElement) {
    return new ImmutableIntegerStack(pNewElement, this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((head == null) ? 0 : head.hashCode());
    result = prime * result + size;
    result = prime * result + ((tail == null) ? 0 : tail.hashCode());
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
    if (!(obj instanceof ImmutableIntegerStack)) {
      return false;
    }
    ImmutableIntegerStack other = (ImmutableIntegerStack) obj;
    if (head == null) {
      if (other.head != null) {
        return false;
      }
    } else if (!head.equals(other.head)) {
      return false;
    }
    if (size != other.size) {
      return false;
    }
    if (tail == null) {
      if (other.tail != null) {
        return false;
      }
    } else if (!tail.equals(other.tail)) {
      return false;
    }
    return true;
  }



}
