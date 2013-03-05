/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.reachdef;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class ReachingDefState implements AbstractState, Serializable {

  private static final long serialVersionUID = -7715698130795640052L;

  public static final ReachingDefState topElement = new ReachingDefState();

  private ReachingDefState stateOnLastFunctionCall;

  private Map<String, Set<DefinitionPoint>> localReachDefs;

  private Map<String, Set<DefinitionPoint>> globalReachDefs;

  private ReachingDefState() {}

  public ReachingDefState(Set<String> globalVariableNames) {
    stateOnLastFunctionCall = null;
    localReachDefs = new HashMap<>();
    globalReachDefs = new HashMap<>();
    addVariables(globalReachDefs, globalVariableNames, UninitializedDefinitionPoint.getInstance());
  }

  public ReachingDefState(Map<String, Set<DefinitionPoint>> pLocalReachDefs,
      Map<String, Set<DefinitionPoint>> pGlobalReachDefs, ReachingDefState stateLastFuncCall) {
    stateOnLastFunctionCall = stateLastFuncCall;
    localReachDefs = pLocalReachDefs;
    globalReachDefs = pGlobalReachDefs;
  }

  public ReachingDefState addLocalReachDef(String variableName, CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    return new ReachingDefState(replaceReachDef(localReachDefs, variableName, definition), globalReachDefs,
        stateOnLastFunctionCall);
  }

  public ReachingDefState addGlobalReachDef(String variableName, CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    return new ReachingDefState(localReachDefs, replaceReachDef(globalReachDefs, variableName, definition),
        stateOnLastFunctionCall);
  }

  private Map<String, Set<DefinitionPoint>> replaceReachDef(Map<String, Set<DefinitionPoint>> toChange,
      String variableName,
      ProgramDefinitionPoint definition) {
    Map<String, Set<DefinitionPoint>> changed = new HashMap<>(toChange);
    ImmutableSet<DefinitionPoint> insert = ImmutableSet.of((DefinitionPoint) definition);
    changed.put(variableName, insert);
    return changed;
  }

  public ReachingDefState initVariables(Set<String> uninitVariableNames, Set<String> parameters,
      CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    HashMap<String, Set<DefinitionPoint>> localVarsDef = new HashMap<>();
    addVariables(localVarsDef, uninitVariableNames, UninitializedDefinitionPoint.getInstance());
    addVariables(localVarsDef, parameters, definition);
    return new ReachingDefState(localVarsDef, globalReachDefs, this);
  }

  private void addVariables(Map<String, Set<DefinitionPoint>> addTo, Set<String> variableNames,
      DefinitionPoint definition) {
    ImmutableSet<DefinitionPoint> insert = ImmutableSet.of(definition);
    for (String name : variableNames) {
      addTo.put(name, insert);
    }
  }

  public ReachingDefState pop() {
    return new ReachingDefState(stateOnLastFunctionCall.localReachDefs, globalReachDefs,
        stateOnLastFunctionCall.stateOnLastFunctionCall);
  }

  public Map<String, Set<DefinitionPoint>> getLocalReachingDefinitions() {
    return this == topElement ? null : localReachDefs;
  }

  public Map<String, Set<DefinitionPoint>> getGlobalReachingDefinitions() {
    return this == topElement ? null : globalReachDefs;
  }

  public boolean isSubsetOf(ReachingDefState superset) {
    if (superset == this || superset == topElement)
      return true;
    if (stateOnLastFunctionCall != superset.stateOnLastFunctionCall)
      return false;
    boolean isLocalSubset = true;
    isLocalSubset = isSubsetOf(localReachDefs, superset.localReachDefs);
    return isLocalSubset && isSubsetOf(globalReachDefs, superset.globalReachDefs);
  }

  private boolean isSubsetOf(Map<String, Set<DefinitionPoint>> subset, Map<String, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset)
      return true;
    for (String var : subset.keySet()) {
      setSub = subset.get(var);
      setSuper = superset.get(var);
      if (setSuper == null || Sets.intersection(setSub, setSuper).size() != setSub.size())
        return false;
    }
    return true;
  }

  public ReachingDefState union(ReachingDefState toJoin) {
    Map<String, Set<DefinitionPoint>> newLocal = null;
    if (toJoin == this)
      return this;
    if (stateOnLastFunctionCall != toJoin.stateOnLastFunctionCall)
      return topElement;
    Map<String, Set<DefinitionPoint>> resultOfMapUnion;
    boolean changed = false;
    resultOfMapUnion = unionMaps(localReachDefs, toJoin.localReachDefs);
    changed = resultOfMapUnion != localReachDefs;
    newLocal = resultOfMapUnion;

    resultOfMapUnion = unionMaps(globalReachDefs, toJoin.globalReachDefs);
    changed = changed || resultOfMapUnion != globalReachDefs;
    if (changed) {
      assert (newLocal != null);
      return new ReachingDefState(newLocal, resultOfMapUnion, stateOnLastFunctionCall);
    }
    return toJoin;
  }

  private Map<String, Set<DefinitionPoint>> unionMaps(Map<String, Set<DefinitionPoint>> map1,
      Map<String, Set<DefinitionPoint>> map2) {
    Map<String, Set<DefinitionPoint>> newMap = new HashMap<>();
    // every declared local variable of a function, global variable occurs in respective map, possibly undefined
    assert (map1.keySet().equals(map1.keySet()));
    Set<DefinitionPoint> unionResult;
    boolean changed = false;
    for (String var : map1.keySet()) {
      unionResult = Sets.union(map1.get(var), map2.get(var)).immutableCopy();
      if (unionResult.size() != map1.get(var).size()) {
        changed = true;
      }
      newMap.put(var, unionResult);
    }
    assert(map1.keySet().equals(newMap.keySet()));
    if (changed) { return newMap; }
    return map1;
  }

  public interface DefinitionPoint {

  }

  public static class UninitializedDefinitionPoint implements DefinitionPoint, Serializable {

    private static final long serialVersionUID = 6987753908487106524L;
    private static UninitializedDefinitionPoint instance = new UninitializedDefinitionPoint();

    private UninitializedDefinitionPoint() {}

    public static UninitializedDefinitionPoint getInstance() {
      return instance;
    }
  }

  public class ProgramDefinitionPoint implements DefinitionPoint, Serializable {

    private static final long serialVersionUID = -7601382286840053882L;
    private transient CFANode entry;
    private transient CFANode exit;

    public ProgramDefinitionPoint(CFANode pEntry, CFANode pExit) {
      entry = pEntry;
      exit = pExit;
    }

    public CFANode getDefinitionEntryLocation() {
      return entry;
    }

    public CFANode getDefinitionExitLocation() {
      return exit;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((entry == null) ? 0 : entry.hashCode());
      result = prime * result + ((exit == null) ? 0 : exit.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ProgramDefinitionPoint other = (ProgramDefinitionPoint) obj;
      if (entry == null) {
        if (other.entry != null)
          return false;
      } else if (!entry.equals(other.entry))
        return false;
      if (exit == null) {
        if (other.exit != null)
          return false;
      } else if (!exit.equals(other.exit))
        return false;
      return true;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeInt(entry.getNodeNumber());
      out.writeInt(exit.getNodeNumber());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      int nodeNumber = in.readInt();
      entry = GlobalInfo.getInstance().getCFAInfo().getNodeByNodeNumber(nodeNumber);
      nodeNumber = in.readInt();
      exit = GlobalInfo.getInstance().getCFAInfo().getNodeByNodeNumber(nodeNumber);
    }

  }

}
