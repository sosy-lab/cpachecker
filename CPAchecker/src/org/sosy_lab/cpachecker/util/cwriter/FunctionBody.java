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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Iterator;
import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * A function is basically a stack of blocks, where the first element is the
 * outermost block of the function and the last element is the current block.
 */
class FunctionBody implements Iterable<BasicBlock> {

  private final Stack<BasicBlock> stack = new Stack<>();

  public FunctionBody(int pElementId, String pFunctionName) {
    stack.push(new BasicBlock(pElementId, pFunctionName));
  }

  public FunctionBody(FunctionBody oldStack) {
    stack.addAll(oldStack.stack);
  }

  public void enterBlock(int pElementId, CAssumeEdge pEdge, String pConditionString) {
    BasicBlock block = new BasicBlock(pElementId, pEdge, pConditionString);
    stack.peek().write(block); // register the inner block in its outer block
    stack.push(block);
  }

  public void leaveBlock() {
    stack.pop();
  }

  public BasicBlock getCurrentBlock() {
    return stack.peek();
  }

  public int size() {
    return stack.size();
  }

  @Override
  public Iterator<BasicBlock> iterator() {
    return stack.iterator();
  }

  public void write(String s) {
    stack.peek().write(s);
  }

  @Override
  public String toString() {
    // To write the C code, we need only the outermost block of the function.
    // It will print its nested blocks automatically as needed.
    return stack.get(0).getCode();
  }
}
