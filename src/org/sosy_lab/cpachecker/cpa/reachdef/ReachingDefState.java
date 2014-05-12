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
package org.sosy_lab.cpachecker.cpa.reachdef;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefinitionStorage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class ReachingDefState implements AbstractState, Serializable {

  private static final long serialVersionUID = -7715698130795640052L;

  private static final SerialProxyReach proxy = new SerialProxyReach();

  public static final ReachingDefState topElement = new ReachingDefState();

  private ReachingDefState stateOnLastFunctionCall;

  private transient Map<String, Set<DefinitionPoint>> localReachDefs;

  private transient Map<String, Set<DefinitionPoint>> globalReachDefs;

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
    if (superset == this || superset == topElement) {
      return true;
    }
    if (stateOnLastFunctionCall != superset.stateOnLastFunctionCall
        && !compareStackStates(stateOnLastFunctionCall, superset.stateOnLastFunctionCall)) {
      return false;
    }
    boolean isLocalSubset;
    isLocalSubset = isSubsetOf(localReachDefs, superset.localReachDefs);
    return isLocalSubset && isSubsetOf(globalReachDefs, superset.globalReachDefs);
  }

  private boolean compareStackStates(ReachingDefState sub, ReachingDefState sup) {
    boolean result;
    do {
      if (sub == null || sup == null) {
        return false;
      }
      result = isSubsetOf(sub.getLocalReachingDefinitions(), sup.getLocalReachingDefinitions());
      result = result && isSubsetOf(sub.getGlobalReachingDefinitions(), sup.getGlobalReachingDefinitions());
      sub = sub.stateOnLastFunctionCall;
      sup = sup.stateOnLastFunctionCall;
    } while (sub != sup && result);
    return result;
  }

  private boolean isSubsetOf(Map<String, Set<DefinitionPoint>> subset, Map<String, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset) {
      return true;
    }
    for (String var : subset.keySet()) {
      setSub = subset.get(var);
      setSuper = superset.get(var);
      if (setSub == setSuper) {
        continue;
      }
      if (setSuper == null || Sets.intersection(setSub, setSuper).size()!=setSub.size()) {
        return false;
      }
    }
    return true;
  }

  public ReachingDefState union(ReachingDefState toJoin) {
    Map<String, Set<DefinitionPoint>> newLocal = null;
    boolean changed = false;
    ReachingDefState lastFunctionCall = stateOnLastFunctionCall;
    if (toJoin == this) {
      return this;
    }
    if (toJoin == topElement || this == topElement) {
      return topElement;
    }
    if (stateOnLastFunctionCall != toJoin.stateOnLastFunctionCall) {
      lastFunctionCall = mergeStackStates(stateOnLastFunctionCall, toJoin.stateOnLastFunctionCall);
      if (lastFunctionCall == topElement) {
        return topElement;
      }
      if (lastFunctionCall != stateOnLastFunctionCall) {
        changed = true;
      }
    }
    Map<String, Set<DefinitionPoint>> resultOfMapUnion;
    resultOfMapUnion = unionMaps(localReachDefs, toJoin.localReachDefs);
    if (resultOfMapUnion == localReachDefs) {
      newLocal = toJoin.localReachDefs;
    } else {
      changed = true;
      newLocal = resultOfMapUnion;
    }

    resultOfMapUnion = unionMaps(globalReachDefs, toJoin.globalReachDefs);
    if (resultOfMapUnion == globalReachDefs) {
      resultOfMapUnion = toJoin.globalReachDefs;
    } else {
      changed = true;
    }

    if (changed) {
      assert (newLocal != null);
      return new ReachingDefState(newLocal, resultOfMapUnion, lastFunctionCall);
    }
    return toJoin;
  }

  private ReachingDefState mergeStackStates(ReachingDefState e1, ReachingDefState e2) {
    Vector<ReachingDefState> statesToMerge = new Vector<>();
    do {
      if (e1.stateOnLastFunctionCall == null || e2.stateOnLastFunctionCall == null) {
        return topElement;
      }
      statesToMerge.add(e1);
      statesToMerge.add(e2);
      e1 = e1.stateOnLastFunctionCall;
      e2 = e2.stateOnLastFunctionCall;
    } while (e1 != e2);

    boolean changed = false;
    Map<String, Set<DefinitionPoint>> resultOfMapUnion;
    Map<String, Set<DefinitionPoint>> newLocal;
    ReachingDefState newStateOnLastFunctionCall = e1;

    for (int i = statesToMerge.size() - 1; i >= 0; i = i - 2) {
      resultOfMapUnion = unionMaps(statesToMerge.get(i - 1).localReachDefs, statesToMerge.get(i).localReachDefs);
      changed = changed || resultOfMapUnion != statesToMerge.get(i - 1).localReachDefs;
      newLocal = resultOfMapUnion;

      resultOfMapUnion = unionMaps(statesToMerge.get(i - 1).globalReachDefs, statesToMerge.get(i).globalReachDefs);
      changed = changed || resultOfMapUnion != statesToMerge.get(i - 1).globalReachDefs;

      newStateOnLastFunctionCall = new ReachingDefState(newLocal, resultOfMapUnion, newStateOnLastFunctionCall);
    }

    if (changed) { return newStateOnLastFunctionCall; }
    return statesToMerge.get(0);
  }

  private Map<String, Set<DefinitionPoint>> unionMaps(Map<String, Set<DefinitionPoint>> map1,
      Map<String, Set<DefinitionPoint>> map2) {
    Map<String, Set<DefinitionPoint>> newMap = new HashMap<>();
    // every declared local variable of a function, global variable occurs in respective map, possibly undefined
    assert (map1.keySet().equals(map2.keySet()));
    if (map1==map2) {
      return map1;
    }
    Set<DefinitionPoint> unionResult;
    boolean changed = false;
    for (String var : map1.keySet()) {
      // decrease merge time, avoid building union if unnecessary
      if (map1.get(var)== map2.get(var)) {
        newMap.put(var, map2.get(var));
        continue;
      }
      unionResult = unionSets(map1.get(var), map2.get(var));
      if (unionResult.size() != map2.get(var).size()) {
        changed = true;
      }
      newMap.put(var, unionResult);
    }
    assert (map1.keySet().equals(newMap.keySet()));
    if (changed) { return newMap; }
    return map1;
  }

  private Set<DefinitionPoint> unionSets(Set<DefinitionPoint> set1, Set<DefinitionPoint> set2) {
    HashSet<DefinitionPoint> result = new HashSet<>();
    for (DefinitionPoint p : set1) {
      result.add(p);
    }
    for (DefinitionPoint p : set2) {
      result.add(p);
    }
    return result;
  }

  private Object writeReplace() throws ObjectStreamException {
    if (this==topElement) {
      return proxy;
    }else{
      return this;

    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    out.writeInt(ReachingDefinitionStorage.getInstance().saveMap(localReachDefs));
    out.writeInt(ReachingDefinitionStorage.getInstance().saveMap(globalReachDefs));
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    int id;

    id = in.readInt();
    localReachDefs = ReachingDefinitionStorage.getInstance().getMap(id);

    id = in.readInt();
    globalReachDefs = ReachingDefinitionStorage.getInstance().getMap(id);
  }


  private static class SerialProxyReach implements Serializable {

    private static final long serialVersionUID = 2843708585446089623L;

    public SerialProxyReach() {}

    private Object readResolve() throws ObjectStreamException {
      return topElement;
    }
  }

  public interface DefinitionPoint {

  }

  public static class UninitializedDefinitionPoint implements DefinitionPoint, Serializable {

    private static final long serialVersionUID = 6987753908487106524L;
    private static UninitializedDefinitionPoint instance = new UninitializedDefinitionPoint();
    private static final SerialProxy writeReplace = new SerialProxy();

    private UninitializedDefinitionPoint() {}

    public static UninitializedDefinitionPoint getInstance() {
      return instance;
    }

    @Override
    public String toString() {
      return "?";
    }

    private Object writeReplace() throws ObjectStreamException {
      return writeReplace;
    }

    private static class SerialProxy implements Serializable {

      private static final long serialVersionUID = 2843708585446089623L;

      public SerialProxy() {}

      private Object readResolve() throws ObjectStreamException {
        return instance;
      }
    }
  }

  public static class ProgramDefinitionPoint implements DefinitionPoint, Serializable {

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
    public String toString() {
      return "(N" + entry.getNodeNumber() + ",N" + exit.getNodeNumber() + ")";
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
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ProgramDefinitionPoint other = (ProgramDefinitionPoint) obj;
      if (entry == null) {
        if (other.entry != null) {
          return false;
        }
      } else if (!entry.equals(other.entry)) {
        return false;
      }
      if (exit == null) {
        if (other.exit != null) {
          return false;
        }
      } else if (!exit.equals(other.exit)) {
        return false;
      }
      return true;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeInt(entry.getNodeNumber());
      out.writeInt(exit.getNodeNumber());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      int nodeNumber = in.readInt();
      CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();
      entry = cfaInfo.getNodeByNodeNumber(nodeNumber);
      nodeNumber = in.readInt();
      exit = cfaInfo.getNodeByNodeNumber(nodeNumber);
    }

  }

}
