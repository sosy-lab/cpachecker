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
package cpa.uninitvars;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import common.Pair;

import cpa.common.interfaces.AbstractElement;

/**
 * @author Philipp Wendler
 */
public class UninitializedVariablesElement implements AbstractElement {

  private final Set<String> globalVars;
  private final Deque<Pair<String, Set<String>>> localVars;
  
  public UninitializedVariablesElement(String entryFunction) {
    globalVars = new HashSet<String>();
    localVars = new LinkedList<Pair<String, Set<String>>>();
    // create context of the entry function
    callFunction(entryFunction);
  }
  
  public UninitializedVariablesElement(Set<String> globalVars, Deque<Pair<String, Set<String>>> localVars) {
    this.globalVars = globalVars;
    this.localVars = localVars;
  }
  
  public void addGlobalVariable(String name) {
    globalVars.add(name);
  }
  
  public void removeGlobalVariable(String name) {
    globalVars.remove(name);
  }
  
  public Set<String> getGlobalVariables() {
    return globalVars;
  }
  
  public void addLocalVariable(String name) {
    localVars.peekLast().getSecond().add(name);
  }
  
  public void removeLocalVariable(String name) {
    localVars.peekLast().getSecond().remove(name);
  }
  
  public Set<String> getLocalVariables() {
    return localVars.peekLast().getSecond();
  }
  
  public Deque<Pair<String, Set<String>>> getallLocalVariables() {
    return localVars;
  }
  
  public boolean isUninitialized(String variable) {
    return globalVars.contains(variable)
        || localVars.peekLast().getSecond().contains(variable);
  }
  
  public void callFunction(String functionName) {
    localVars.addLast(new Pair<String, Set<String>>(functionName, new HashSet<String>()));
  }
  
  public void returnFromFunction() {
    localVars.pollLast();
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof UninitializedVariablesElement)) {
      return false;
    }
    if (this == o) {
      return true;
    }
    
    UninitializedVariablesElement otherElement = (UninitializedVariablesElement)o;
    
    return globalVars.equals(otherElement.globalVars)
        && localVars.equals(otherElement.localVars);
  }
  
  @Override
  public int hashCode() {
    return localVars.hashCode();
  }
  
  @Override
  protected UninitializedVariablesElement clone() {
    LinkedList<Pair<String, Set<String>>> newLocalVars = new LinkedList<Pair<String, Set<String>>>();
    
    for (Pair<String, Set<String>> localSet : localVars) {
      newLocalVars.addLast(new Pair<String, Set<String>>(localSet.getFirst(),
                                                     new HashSet<String>(localSet.getSecond())));
    }
    
    return new UninitializedVariablesElement(new HashSet<String>(globalVars), newLocalVars);
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[<global:");
    for (String var : globalVars) {
      sb.append(" " + var + " ");
    }
    for (Pair<String, Set<String>> stackframe: localVars) {
      sb.append("> <" + stackframe.getFirst() + ":");
      for (String var : stackframe.getSecond()) {
        sb.append(" " + var + " ");
      }
    }
    sb.append(">]");
    return sb.toString();
  }
}
