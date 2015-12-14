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

import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import com.google.common.base.Preconditions;

public class ImmutableLoopStack implements ImmutableStack<Loop> {

  @Nullable private final ImmutableLoopStack tail;
  @Nullable private final Loop head;
  private final int size;

  private static ImmutableLoopStack EMPTYSTACK = new ImmutableLoopStack();

  private ImmutableLoopStack(final Loop pHead, final ImmutableLoopStack pTail) {
    head = Preconditions.checkNotNull(pHead);
    tail = Preconditions.checkNotNull(pTail);
    size = pTail.size() + 1;
  }

  private ImmutableLoopStack() {
    head = null;
    tail = null;
    size = 0;
  }

  public static ImmutableLoopStack singleton(Loop pElement) {
    return new ImmutableLoopStack(pElement, EMPTYSTACK);
  }

  public static ImmutableLoopStack empty() {
    return EMPTYSTACK;
  }

  @Override
  public ImmutableLoopStack getTail() {
    return tail;
  }

  @Override
  public Loop peekHead() {
    return head;
  }

  @Override
  public boolean isEmpty() {
    return head != null;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public ImmutableLoopStack push(Loop pNewElement) {
    return new ImmutableLoopStack(pNewElement, this);
  }

  @Override
  public String toString() {
    if (peekHead() == null) {
      return "Loop stack empty";
    } else {
      return String.format("Loop starting at node %s, nested in depth %d", peekHead().getLoopHeads(), size());
    }
  }

}
