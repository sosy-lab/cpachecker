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
package org.sosy_lab.cpachecker.util.invariants.balancer.prh12;

import java.util.Stack;



public class ColumnChoiceStack {

  Stack<ColumnChoiceFrame> stack;

  ColumnChoiceStack(ColumnChoiceFrame cf) {
    stack = new Stack<>();
    stack.push(cf);
  }

  boolean topFrameIsComplete() {
    if (stack.empty()) {
      return false;
    }
    ColumnChoiceFrame cf = stack.peek();
    return cf.isComplete();
  }

  /*
   * Ask the top frame to make its next choice. If it makes one, it produces a new frame, and we
   * push that onto the stack. If it does not, then we do nothing.
   */
  void makeNextFrameChoice() {
    if (stack.empty()) {
      return;
    }
    ColumnChoiceFrame cf = stack.peek();
    ColumnChoiceFrame next = cf.next();
    if (next != null) {
      stack.push(next);
    }
  }

}
