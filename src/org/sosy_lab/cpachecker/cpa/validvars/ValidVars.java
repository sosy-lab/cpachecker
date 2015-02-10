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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class ValidVars implements Serializable {

  private static final long serialVersionUID = 3944327361058660L;

  private final ImmutableSet<String> globalValidVars;
  private final ImmutableMap<String, Set<String>> localValidVars;
  private final ImmutableMap<String,Byte> numFunctionCalled;

  public static final ValidVars initial = new ValidVars(Collections.<String> emptySet(),
      Collections.<String, Set<String>> emptyMap(), Collections.<String, Byte> emptyMap());

  ValidVars(Set<String> pGlobalValidVars, Map<String, ? extends Set<String>> pLocal, Map<String,Byte> pNumFunctionCalled) {
    globalValidVars = ImmutableSet.copyOf(pGlobalValidVars);
    localValidVars = ImmutableMap.copyOf(pLocal);
    numFunctionCalled = ImmutableMap.copyOf(pNumFunctionCalled);
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
    if (newGlobals.size()!=pOther.globalValidVars.size()) {
      changed = true;
    }

    // merge local vars
    ImmutableSet<String> newLocalsForFun;
    ImmutableMap.Builder<String,ImmutableSet<String>> builderMap = ImmutableMap.builder();
    for (String funName: localValidVars.keySet()) {
      checkArgument(numFunctionCalled.get(funName).equals(pOther.numFunctionCalled.get(funName)), "Require Callstack CPA to separate different function calls.");
      builder = ImmutableSet.builder();
      builder.addAll(pOther.localValidVars.get(funName));
      builder.addAll(localValidVars.get(funName));

      newLocalsForFun = builder.build();
      if (newLocalsForFun.size()!=pOther.localValidVars.get(funName).size()) {
        changed = true;
      }

      builderMap.put(funName, newLocalsForFun);
    }

    if (changed) {
      return new ValidVars(newGlobals, builderMap.build(), numFunctionCalled);
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
      return new ValidVars(builder.build(), localValidVars, numFunctionCalled);
    }
    return this;
  }

  public ValidVars extendLocalVars(String funName, String newLocalVarName) {
    return extendLocalVars(funName, ImmutableSet.of(newLocalVarName));
  }

  private ValidVars extendLocalVars(String funName, Collection<String> newLocalVarsNames) {
    if (newLocalVarsNames != null) {
      return new ValidVars(globalValidVars, updateLocalVars(funName, newLocalVarsNames), numFunctionCalled);
    }
    return this;
  }

  private Map<String, Set<String>> updateLocalVars(String funName, Collection<String> newLocalVarsNames) {
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
      return builderMap.build();
    }
    return localValidVars;
  }

  public ValidVars extendLocalVarsFunctionCall(String funName, Collection<String> newLocalVarsNames) {
    if (newLocalVarsNames != null) {
      return new ValidVars(globalValidVars, updateLocalVars(funName, newLocalVarsNames), increaseNumForFunction(funName));
    }
    return this;
  }

  public ValidVars removeVarsOfFunction(String funName) {
    if (localValidVars != null && localValidVars.containsKey(funName)) {
      if (numFunctionCalled.get(funName) > 1) { return new ValidVars(globalValidVars, localValidVars,
          decreaseNumForFunction(funName)); }
      ImmutableMap.Builder<String, Set<String>> builderMap = ImmutableMap.builder();
      for (String functionName : localValidVars.keySet()) {
        if (!functionName.equals(funName)) {
          builderMap.put(functionName, localValidVars.get(functionName));
        }
      }
      return new ValidVars(globalValidVars, builderMap.build(), decreaseNumForFunction(funName));
    }
    return this;
  }

  private Map<String, Byte> decreaseNumForFunction(String pFunctionName) {
    ImmutableMap.Builder<String, Byte> builder = ImmutableMap.builder();
    for (String functionName : numFunctionCalled.keySet()) {
      if (!functionName.equals(pFunctionName)) {
        builder.put(functionName, numFunctionCalled.get(functionName));
      } else {
        if (numFunctionCalled.get(functionName) > 1) {
          builder.put(functionName, (byte) (numFunctionCalled.get(functionName).byteValue() - 1));
        }
      }
    }
    return builder.build();
  }

  private Map<String, Byte> increaseNumForFunction(String pFunctionName) {
    ImmutableMap.Builder<String, Byte> builder = ImmutableMap.builder();
    for (String functionName : numFunctionCalled.keySet()) {
      if (!functionName.equals(pFunctionName)) {
        builder.put(functionName, numFunctionCalled.get(functionName));
      } else {
        builder.put(functionName, (byte) (numFunctionCalled.get(functionName).byteValue() + 1));
      }
    }
    if (!numFunctionCalled.containsKey(pFunctionName)) {
      builder.put(pFunctionName, (byte) 1);
    }
    return builder.build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append("global:\n");
    sb.append(globalValidVars.toString());
    sb.append("\n");
    Joiner.on("\n").withKeyValueSeparator(":\n").appendTo(sb, localValidVars);
    sb.append(")");
    return sb.toString();
  }

  public String toStringInDOTFormat(){
    StringBuilder sb = new StringBuilder();
    sb.append("(\\n");
    sb.append("global:\\n");
    sb.append(globalValidVars.toString());
    sb.append("\\n");
    Joiner.on("\\n").withKeyValueSeparator(":\\n").appendTo(sb, localValidVars);
    sb.append(")");
    return sb.toString();
  }
}
