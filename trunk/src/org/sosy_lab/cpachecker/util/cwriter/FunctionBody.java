// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * A function is basically a stack of blocks, where the first element is the outermost block of the
 * function and the last element is the current block.
 */
class FunctionBody implements Iterable<BasicBlock> {

  private final Deque<BasicBlock> stack = new ArrayDeque<>();

  public FunctionBody(int pElementId, String pFunctionName) {
    stack.push(new BasicBlock(pElementId, pFunctionName));
  }

  public FunctionBody(FunctionBody oldStack) {
    stack.addAll(oldStack.stack);
  }

  public void enterBlock(int pElementId, CAssumeEdge pEdge, String pConditionString) {
    BasicBlock block = new BasicBlock(pElementId, pEdge, pConditionString);
    stack.getLast().write(block); // register the inner block in its outer block
    stack.addLast(block);
  }

  public void leaveBlock() {
    stack.removeLast();
  }

  public BasicBlock getCurrentBlock() {
    return stack.getLast();
  }

  public int size() {
    return stack.size();
  }

  @Override
  public Iterator<BasicBlock> iterator() {
    return stack.iterator();
  }

  public void write(String s) {
    stack.getLast().write(s);
  }

  @Override
  public String toString() {
    // To write the C code, we need only the outermost block of the function.
    // It will print its nested blocks automatically as needed.
    return stack.getFirst().getCode();
  }
}
