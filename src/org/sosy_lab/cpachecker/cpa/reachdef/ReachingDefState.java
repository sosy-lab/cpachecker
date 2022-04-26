// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.reachdef;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ReachingDefState
    implements AbstractState, Serializable, LatticeAbstractState<ReachingDefState>, Graphable {

  private static final long serialVersionUID = -7715698130795640052L;

  private static final SerialProxyReach proxy = new SerialProxyReach();

  public static final ReachingDefState topElement = new ReachingDefState();

  private transient Map<MemoryLocation, Set<DefinitionPoint>> localReachDefs;

  private transient Map<MemoryLocation, Set<DefinitionPoint>> globalReachDefs;

  private Map<CExpression, ProgramDefinitionPoint> unhandled = new HashMap<>();

  private ReachingDefState() {}

  public ReachingDefState(Set<MemoryLocation> globalVariableNames) {
    localReachDefs = new HashMap<>();
    globalReachDefs = new HashMap<>();
    addVariables(globalReachDefs, globalVariableNames, UninitializedDefinitionPoint.getInstance());
  }

  public ReachingDefState(
      Map<MemoryLocation, Set<DefinitionPoint>> pLocalReachDefs,
      Map<MemoryLocation, Set<DefinitionPoint>> pGlobalReachDefs) {
    localReachDefs = pLocalReachDefs;
    globalReachDefs = pGlobalReachDefs;
  }

  public ReachingDefState addLocalReachDef(
      MemoryLocation variableName, CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    return new ReachingDefState(
        replaceReachDef(localReachDefs, variableName, definition), globalReachDefs);
  }

  public ReachingDefState addGlobalReachDef(
      MemoryLocation variableName, CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    return new ReachingDefState(
        localReachDefs, replaceReachDef(globalReachDefs, variableName, definition));
  }

  void addUnhandled(CExpression pExp, CFANode pEntry, CFANode pExit) {
    ProgramDefinitionPoint def = new ProgramDefinitionPoint(pEntry, pExit);
    unhandled.put(pExp, def);
  }

  Map<CExpression, ProgramDefinitionPoint> getAndResetUnhandled() {
    Map<CExpression, ProgramDefinitionPoint> unhandledExpressions = unhandled;
    unhandled = new HashMap<>();
    return unhandledExpressions;
  }

  private Map<MemoryLocation, Set<DefinitionPoint>> replaceReachDef(
      Map<MemoryLocation, Set<DefinitionPoint>> toChange,
      MemoryLocation variableName,
      ProgramDefinitionPoint definition) {
    Map<MemoryLocation, Set<DefinitionPoint>> changed = new HashMap<>(toChange);
    ImmutableSet<DefinitionPoint> insert = ImmutableSet.of(definition);
    changed.put(variableName, insert);
    return changed;
  }

  public ReachingDefState initVariables(
      Set<MemoryLocation> uninitVariableNames,
      Set<MemoryLocation> parameters,
      CFANode pEntry,
      CFANode pExit) {
    ProgramDefinitionPoint definition = new ProgramDefinitionPoint(pEntry, pExit);
    Map<MemoryLocation, Set<DefinitionPoint>> localVarsDef = new HashMap<>(localReachDefs);
    addVariables(localVarsDef, uninitVariableNames, UninitializedDefinitionPoint.getInstance());
    addVariables(localVarsDef, parameters, definition);
    return new ReachingDefState(localVarsDef, globalReachDefs);
  }

  private void addVariables(
      Map<MemoryLocation, Set<DefinitionPoint>> addTo,
      Set<MemoryLocation> variableNames,
      DefinitionPoint definition) {
    ImmutableSet<DefinitionPoint> insert = ImmutableSet.of(definition);
    for (MemoryLocation name : variableNames) {
      addTo.put(name, insert);
    }
  }

  public ReachingDefState pop(String pFunctionName) {
    Map<MemoryLocation, Set<DefinitionPoint>> newLocalReachs = new HashMap<>(localReachDefs);
    for (MemoryLocation var : localReachDefs.keySet()) {
      if (var.isOnFunctionStack(pFunctionName)) {
        newLocalReachs.remove(var);
      }
    }

    return new ReachingDefState(newLocalReachs, globalReachDefs);
  }

  public Map<MemoryLocation, Set<DefinitionPoint>> getLocalReachingDefinitions() {
    return this == topElement ? null : localReachDefs;
  }

  public Map<MemoryLocation, Set<DefinitionPoint>> getGlobalReachingDefinitions() {
    return this == topElement ? null : globalReachDefs;
  }

  @Override
  public boolean isLessOrEqual(ReachingDefState superset) {
    if (superset == this || superset == topElement) {
      return true;
    }
    boolean isLocalSubset;
    isLocalSubset = isSubsetOf(localReachDefs, superset.localReachDefs);
    return isLocalSubset && isSubsetOf(globalReachDefs, superset.globalReachDefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(globalReachDefs, localReachDefs);
  }

  @Override
  public boolean equals(Object pO) {
    if (pO instanceof ReachingDefState) {
      ReachingDefState other = (ReachingDefState) pO;
      return Objects.equals(globalReachDefs, other.globalReachDefs)
          && Objects.equals(localReachDefs, other.localReachDefs);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  private boolean isSubsetOf(
      Map<MemoryLocation, Set<DefinitionPoint>> subset,
      Map<MemoryLocation, Set<DefinitionPoint>> superset) {
    Set<DefinitionPoint> setSub, setSuper;
    if (subset == superset) {
      return true;
    }
    for (Entry<MemoryLocation, Set<DefinitionPoint>> entry : subset.entrySet()) {
      setSub = entry.getValue();
      setSuper = superset.get(entry.getKey());
      if (setSub == setSuper) {
        continue;
      }
      if (setSuper == null || Sets.intersection(setSub, setSuper).size() != setSub.size()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ReachingDefState join(ReachingDefState toJoin) {
    Map<MemoryLocation, Set<DefinitionPoint>> newLocal;
    boolean changed = false;
    if (toJoin == this) {
      return this;
    }
    if (toJoin == topElement || this == topElement) {
      return topElement;
    }

    Map<MemoryLocation, Set<DefinitionPoint>> resultOfMapUnion;
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
      assert newLocal != null;
      return new ReachingDefState(newLocal, resultOfMapUnion);
    }
    return toJoin;
  }

  private Map<MemoryLocation, Set<DefinitionPoint>> unionMaps(
      Map<MemoryLocation, Set<DefinitionPoint>> map1,
      Map<MemoryLocation, Set<DefinitionPoint>> map2) {
    Map<MemoryLocation, Set<DefinitionPoint>> newMap = new HashMap<>();
    if (map1 == map2) {
      return map1;
    }
    Set<DefinitionPoint> unionResult;
    boolean changed = false;
    Set<MemoryLocation> variableUnion = Sets.union(map1.keySet(), map2.keySet());
    for (MemoryLocation var : variableUnion) {
      Set<DefinitionPoint> defPoints1 = map1.get(var);
      Set<DefinitionPoint> defPoints2 = map2.get(var);
      // decrease merge time, avoid building union if unnecessary
      if (defPoints1 == defPoints2) {
        assert defPoints1 != null;
        newMap.put(var, defPoints1);
        continue;
      }
      if (defPoints1 == null) {
        defPoints1 = ImmutableSet.of();
      } else if (defPoints2 == null) {
        defPoints2 = ImmutableSet.of();
      }
      unionResult = unionSets(defPoints1, defPoints2);
      if (unionResult.size() != defPoints1.size() || unionResult.size() != defPoints2.size()) {
        assert unionResult.size() >= defPoints1.size() && unionResult.size() >= defPoints2.size()
            : "Union of map1 and map2 shouldn't be able to shrink!";
        changed = true;
      }
      newMap.put(var, unionResult);
    }
    if (changed) {
      return newMap;
    }
    return map1;
  }

  private Set<DefinitionPoint> unionSets(Set<DefinitionPoint> set1, Set<DefinitionPoint> set2) {
    Set<DefinitionPoint> result = new HashSet<>(set1);

    result.addAll(set2);
    return result;
  }

  private Object writeReplace() {
    if (this == topElement) {
      return proxy;
    } else {
      return this;
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    out.writeInt(localReachDefs.size());
    for (Entry<MemoryLocation, Set<DefinitionPoint>> localReach : localReachDefs.entrySet()) {
      out.writeObject(localReach.getKey());
      out.writeObject(localReach.getValue());
    }

    out.writeInt(globalReachDefs.size());
    for (Entry<MemoryLocation, Set<DefinitionPoint>> globalReach : globalReachDefs.entrySet()) {
      out.writeObject(globalReach.getKey());
      out.writeObject(globalReach.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    int size;
    size = in.readInt();
    localReachDefs = Maps.newHashMapWithExpectedSize(size);

    for (int i = 0; i < size; i++) {
      localReachDefs.put((MemoryLocation) in.readObject(), (Set<DefinitionPoint>) in.readObject());
    }

    size = in.readInt();
    globalReachDefs = Maps.newHashMapWithExpectedSize(size);

    for (int i = 0; i < size; i++) {
      globalReachDefs.put((MemoryLocation) in.readObject(), (Set<DefinitionPoint>) in.readObject());
    }
  }

  private static class SerialProxyReach implements Serializable {

    private static final long serialVersionUID = 2843708585446089623L;

    public SerialProxyReach() {}

    private Object readResolve() {
      return topElement;
    }
  }

  public interface DefinitionPoint {}

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

    private Object writeReplace() {
      return writeReplace;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object pO) {
      return pO instanceof UninitializedDefinitionPoint;
    }

    private static class SerialProxy implements Serializable {

      private static final long serialVersionUID = 2843708585446089623L;

      public SerialProxy() {}

      private Object readResolve() {
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
      return Objects.hash(entry, exit);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ProgramDefinitionPoint)) {
        return false;
      }
      ProgramDefinitionPoint other = (ProgramDefinitionPoint) obj;
      return Objects.equals(entry, other.entry) && Objects.equals(exit, other.exit);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeInt(entry.getNodeNumber());
      out.writeInt(exit.getNodeNumber());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException {
      int nodeNumber = in.readInt();
      CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orElseThrow();
      entry = cfaInfo.getNodeByNodeNumber(nodeNumber);
      nodeNumber = in.readInt();
      exit = cfaInfo.getNodeByNodeNumber(nodeNumber);
    }
  }

  @Override
  public String toDOTLabel() {

    StringBuilder sb = new StringBuilder();

    sb.append("{");
    sb.append("\\n");

    sb.append(System.identityHashCode(this));
    sb.append("\\n");

    // create a string like: global:  [varName1; varName2; ... ; ...]
    sb.append("global:");
    sb.append(createStringOfMap(globalReachDefs));
    sb.append("\\n");

    // create a string like: local:  [varName1; varName2; ... ; ...]
    sb.append("local:");
    sb.append(createStringOfMap(localReachDefs));
    sb.append("\\n");

    sb.append("}");

    return sb.toString();
  }

  private String createStringOfMap(Map<MemoryLocation, Set<DefinitionPoint>> map) {
    StringBuilder sb = new StringBuilder();
    sb.append(" [");

    boolean first = true;

    for (Entry<MemoryLocation, Set<DefinitionPoint>> entry : map.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }

      sb.append(" (");
      sb.append(entry.getKey());
      sb.append(": [");
      Joiner.on("; ").appendTo(sb, entry.getValue());
      sb.append("])");
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
