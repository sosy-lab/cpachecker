/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.invariant.controller;

import cfa.objectmodel.CFANode;

/**
 * Representation of a call context as a sequence of function call
 * nodes that led to a given node.
 * @author g.theoduloz
 */
public abstract class Context {
  
  /** Empty context */
  public static class Empty extends Context {
    public static final Empty INSTANCE = new Empty();
    private Empty() { }
    @Override
    public CFANode getTop() {
      return null;
    }
    @Override
    public Context pop() {
      return this;
    }
    @Override
    public int getSize() {
      return 0;
    }
    @Override
    public boolean sameContext(Context other) {
      return other == this;
    }
    @Override
    public String toString() {
      return ":";
    }
  }
  public static final Context EMPTY = Empty.INSTANCE;
  
  /** Non-empty context */
  public static class Cons extends Context{
    private final CFANode head;
    private final Context tail;
    
    public Cons(CFANode h, Context t) {
      head = h;
      tail = t;
    }
    
    @Override
    public int getSize() {
      return 1 + tail.getSize();
    }

    @Override
    public CFANode getTop() {
      return head;
    }

    @Override
    public Context pop() {
      return tail;
    }
    
    @Override
    public boolean sameContext(Context other) {
      if (other == EMPTY) {
        return false;
      } else {
        Cons cother = (Cons)other;
        if (head == cother.head)
          return (tail.sameContext(cother.tail));
        else
          return false;
      }
    }
    
    @Override
    public int hashCode() {
      return head.hashCode() + tail.hashCode();
    }
    
    @Override
    public String toString() {
      return Integer.toString(head.getNodeNumber()) + ":" + tail.toString();
    }
  }
  
  /** Is this context the same as the given context */
  public abstract boolean sameContext(Context other);
  
  /**
   * Append the given non-null node to the context and return the
   * resulting context
   */
  public Context push(CFANode node) {
    assert node != null;
    return new Cons(node, this);
  }
  
  /**
   * Pop the last element from the context (or do nothing
   * if it is empty)
   */
  public abstract Context pop();
  
  /** Return the top of the context, or null if it is empty */
  public abstract CFANode getTop();
  
  /** Return the size of the context */
  public abstract int getSize();
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof Context)
      return sameContext((Context) other);
    else
      return false;
  }
}
