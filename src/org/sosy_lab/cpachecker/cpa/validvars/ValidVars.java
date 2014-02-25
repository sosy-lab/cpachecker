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
package org.sosy_lab.cpachecker.cpa.validvars;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class ValidVars {

  private final ImmutableSet<String> globalValidVars;
  private final ImmutableMap<String, Set<String>> localValidVars;

  public static final ValidVars initial = new ValidVars(Collections.<String> emptySet(),
      Collections.<String, Set<String>> emptyMap());

  ValidVars(Set<String> pGlobalValidVars, Map<String, ? extends Set<String>> pLocal){
    globalValidVars = ImmutableSet.copyOf(pGlobalValidVars);
    localValidVars = ImmutableMap.copyOf(pLocal);
  }

  public boolean containsVar(String varName) {
    String[] split = varName.split("::");
    if (split.length > 1) {
      Set<String> functionVars = localValidVars.get(split[0]);
      return functionVars != null ? functionVars.contains(split[1]) : false;
    } else {
      return globalValidVars.contains(varName);
    }
  }

  public ValidVars mergeWith(ValidVars pOther) throws CPAException{
    if (!pOther.localValidVars.keySet().containsAll(localValidVars.keySet())) {
      throw new CPAException("Require Callstack CPA to separate different function calls and Location CPA to separate different locations.");
    }

    boolean changed = false;

    // merge global vars
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    builder.addAll(pOther.globalValidVars);
    builder.addAll(globalValidVars);

    ImmutableSet<String> newGlobals = builder.build();
    if(newGlobals.size()!=pOther.globalValidVars.size()){
      changed = true;
    }

    // merge local vars
    ImmutableSet<String> newLocalsForFun;
    ImmutableMap.Builder<String,ImmutableSet<String>> builderMap = ImmutableMap.builder();
    for(String funName: localValidVars.keySet()){
      builder = ImmutableSet.builder();
      builder.addAll(pOther.localValidVars.get(funName));
      builder.addAll(localValidVars.get(funName));

      newLocalsForFun = builder.build();
      if(newLocalsForFun.size()!=pOther.localValidVars.get(funName).size()){
        changed = true;
      }

      builderMap.put(funName, newLocalsForFun);
    }

    if(changed){
      return new ValidVars(newGlobals, builderMap.build());
    }

    return pOther;
  }

  public boolean isSubsetOf(ValidVars pOther) {
    boolean subsetLocal = false;

    if (pOther.localValidVars.keySet().containsAll(localValidVars.keySet())) {
      for (String funName : localValidVars.keySet()) {
        if (!pOther.localValidVars.get(funName).containsAll(localValidVars.get(funName))) { return false; }
      }
      subsetLocal = true;
    }

    return subsetLocal && pOther.globalValidVars.containsAll(globalValidVars);
  }

  public ValidVars extendGlobalVars(String varName) {
    if (varName != null) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      builder.addAll(globalValidVars);
      builder.add(varName);
      return new ValidVars(builder.build(), localValidVars);
    }
    return this;
  }

  public ValidVars extendLocalVars(String funName, String newLocalVarName) {
    return extendLocalVars(funName, ImmutableSet.of(newLocalVarName));
  }

  public ValidVars extendLocalVars(String funName, Collection<String> newLocalVarsNames){
    if (newLocalVarsNames != null) {
      ImmutableMap.Builder<String, Set<String>> builderMap = ImmutableMap.builder();
      for (String functionName : localValidVars.keySet()) {
        if (!functionName.equals(funName)) {
          builderMap.put(functionName, localValidVars.get(functionName));
        }
      }

      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      if (localValidVars.containsKey(funName)) {
        builder.addAll(localValidVars.get(funName));
      }
      builder.addAll(newLocalVarsNames);
      builderMap.put(funName, builder.build());

      return new ValidVars(globalValidVars, builderMap.build());
    }
    return this;
  }

  public ValidVars removeVarsOfFunction(String funName) {
    if (localValidVars.containsKey(funName)) {
      ImmutableMap.Builder<String, Set<String>> builderMap = ImmutableMap.builder();
      for (String functionName : localValidVars.keySet()) {
        if (!functionName.equals(funName)) {
          builderMap.put(functionName, localValidVars.get(functionName));
        }
      }
      return new ValidVars(globalValidVars, builderMap.build());
    }
    return this;
  }

}
