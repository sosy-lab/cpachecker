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
package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;

class CallstackElement implements AbstractElement, Partitionable {
  
  private final CallstackElement previousElement;
  private final String currentFunction;
  private final CFANode callerNode;
  
  public CallstackElement(CallstackElement previousElement, String function, CFANode callerNode) {
    this.previousElement = previousElement;
    this.currentFunction = checkNotNull(function);
    this.callerNode = checkNotNull(callerNode);
  }
  
  public CallstackElement getPreviousElement() {
    return previousElement;
  }
  
  public String getCurrentFunction() {
    return currentFunction;
  }
  
  public CFANode getCallNode() {
    return callerNode;
  }
  
  @Override
  public Object getPartitionKey() {
    return this;
  }
  
  @Override
  public String toString() {
    return "Function " + getCurrentFunction()
         + " called from node " + getCallNode()
         + " [" + Integer.toHexString(super.hashCode()) + "]";
  }
  
  // no equals and hashCode because each instance represents a unique abstract state!
  
}