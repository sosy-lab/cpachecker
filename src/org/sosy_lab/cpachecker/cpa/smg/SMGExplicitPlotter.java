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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitValueBase;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;


public final class SMGExplicitPlotter {
  private final HashMap <Location, String> locationIndex = new HashMap<>();
  private int offset = 0;
  private final ExplicitState explicitState;
  private final SMGState smgState;

  public SMGExplicitPlotter(ExplicitState pExplicitState, SMGState pSmgState) {
    explicitState = pExplicitState;
    smgState = pSmgState;
  }

  public String smgAsDot(CLangSMG smg, String name, String location) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph gr_" + name.replace('-', '_') + "{\n");
    offset += 2;
    sb.append(newLineWithOffset("label = \"Location: " + location.replace("\"", "") + "\";"));

    addStackSubgraph(smg, sb);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      sb.append(newLineWithOffset(smgObjectAsDot(heapObject, smg)));
      locationIndex.put(Location.valueOf(heapObject), heapObject.getLabel());
    }

    // This only works after creating all heap Objects,
    // because we can't differentiate between global Memlocs and heap Memlocs
    addGlobalObjectSubgraph(smg, sb);

    Map<Integer, MemoryLocation> coveredBySMG = new HashMap<>();
    Set<MemoryLocation> coveredMemloc = new HashSet<>();

    for (SMGEdgeHasValue edge : smg.getHVEdges()) {

      SMGObject obj = edge.getObject();
      String functionName = smg.getFunctionName(obj);
      MemoryLocation memloc = smgState.resolveMemLoc(SMGAddress.valueOf(obj, edge.getOffset()), functionName);
      if (explicitState.contains(memloc)) {
        coveredBySMG.put(edge.getValue(), memloc);
        coveredMemloc.add(memloc);
      }
    }

    for (int value : smg.getValues()) {
      sb.append(newLineWithOffset(smgValueAsDot(value, coveredBySMG)));
    }

    Set<MemoryLocation> notCoveredBySMG = new HashSet<>();

    for(MemoryLocation memloc : explicitState.getTrackedMemoryLocations()) {
      // We don't consider values from the old Nomenclature in explicit cpa
      if(!coveredMemloc.contains(memloc) && !memloc.getAsSimpleString().contains("->")) {
        sb.append(newLineWithOffset(explicitValueAsDot(memloc)));
        notCoveredBySMG.add(memloc);
      }
    }

    for (SMGEdgeHasValue edge: smg.getHVEdges()) {
      sb.append(newLineWithOffset(smgHVEdgeAsDot(edge, smg)));
    }

    for (MemoryLocation memloc : notCoveredBySMG) {
      sb.append(newLineWithOffset(memlocAsDot(memloc)));
    }

    for (SMGEdgePointsTo edge: smg.getPTEdges().values()) {
      sb.append(newLineWithOffset(smgPTEdgeAsDot(edge, smg)));
    }

    sb.append("}");

    return sb.toString();
  }

  private String memlocAsDot(MemoryLocation pMemloc) {
    return locationIndex.get(Location.valueOf(pMemloc)) + " -> expValue_" + explicitState.getValueFor(pMemloc) + "[label=\"[" + pMemloc.getOffset() + "]\"];";
  }

  private String explicitValueAsDot(MemoryLocation pMemloc) {
    ExplicitValueBase value = explicitState.getValueFor(pMemloc);
    return "expValue_" + value.toString() + "[label=\"" + value.toString() + "\"];";
  }

  private void addStackSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_stack {"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Stack\";"));

    int i = 0;
    for (CLangStackFrame stack_item : pSmg.getStackFrames()) {
      addStackItemSubgraph(stack_item, pSb, i);
      i++;
    }
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private void addStackItemSubgraph(CLangStackFrame pStackFrame, StringBuilder pSb, int pIndex) {

    String functionName = pStackFrame.getFunctionDeclaration().getName();

    pSb.append(newLineWithOffset("subgraph cluster_stack_" + functionName + "{"));
    offset += 2;
    pSb.append(newLineWithOffset("fontcolor=blue;"));
    pSb.append(newLineWithOffset("label=\"" + pStackFrame.getFunctionDeclaration().toASTString() + "\";"));

    pSb.append(newLineWithOffset(smgScopeFrameAsDot(pStackFrame.getVariables(), String.valueOf(pIndex), functionName)));

    offset -= 2;
    pSb.append(newLineWithOffset("}"));

  }

  @Nullable
  private String smgScopeFrameAsDot(Map<String, SMGRegion> pNamespace, String pStructId, String pFunctionName) {
    StringBuilder sb = new StringBuilder();
    sb.append("struct" + pStructId + "[shape=record,label=\" ");

    // I sooo wish for Python list comprehension here...
    ArrayList<String> nodes = new ArrayList<>();
    for (String key : pNamespace.keySet()) {
      SMGObject obj = pNamespace.get(key);

      if (key.equals("node")) {
        // escape Node1
        key = "node1";
      }

      nodes.add("<" + key + "> " + obj.toString());
      Location location = Location.valueOf(obj, pFunctionName);
      locationIndex.put(location, "struct" + pStructId + ":" + key);
    }

    Set<MemoryLocation> memoryLocations;

    if(pFunctionName == null) {
      memoryLocations = explicitState.getGlobalMemoryLocations();
    } else {
      memoryLocations = explicitState.getMemoryLocationsOnStack(pFunctionName);
    }

    for (MemoryLocation memloc : memoryLocations) {
      Location location = Location.valueOf(memloc);
      //  We don't consider values written into explicit cpa under the old
      //  Nomenclature
      if (!locationIndex.containsKey(location) && !location.location.contains("->")) {
        // We don't know the size of the memory location
        nodes.add("<" + memloc.getIdentifier() + "> " + memloc.getIdentifier());
        locationIndex.put(location, "struct" + pStructId + ":" + memloc.getIdentifier());
      }
    }

    sb.append(Joiner.on(" | ").join(nodes));
    sb.append("\"];\n");
    return sb.toString();
  }

  private void addGlobalObjectSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_global{"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Global objects\";"));
    pSb.append(newLineWithOffset(smgScopeFrameAsDot(pSmg.getGlobalObjects(), "global", null)));
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private String smgHVEdgeAsDot(SMGEdgeHasValue pEdge, CLangSMG smg) {
    SMGObject obj = pEdge.getObject();
    Location location = Location.valueOf(obj, smg.getFunctionName(obj));

    return locationIndex.get(location) + " -> value_" + pEdge.getValue() + "[label=\"[" + pEdge.getOffset() + "]\"];";
  }

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge, CLangSMG smg) {

    SMGObject obj = pEdge.getObject();
    Location location = Location.valueOf(obj, smg.getFunctionName(obj));

    return "value_" + pEdge.getValue() + " -> " + locationIndex.get(location) + "[label=\"+" + pEdge.getOffset()
        + "b\"];";
  }

  private static String smgObjectAsDot(SMGObject pObject, CLangSMG pSmg) {

    String valid = pSmg.isObjectValid(pObject) ? "" : " : invalid ";
    return pObject.getLabel() + " [ shape=rectangle, label = \"" + pObject.toString() + valid + "\"];";
  }

  private String smgValueAsDot(int value, Map<Integer, MemoryLocation> pCoveredBySMG) {

    if (pCoveredBySMG.containsKey(value)) {
      return "value_" + value + "[label=\"#" + value + " : "
          + explicitState.getValueFor(pCoveredBySMG.get(value)) + "\"];";
    } else {
      return "value_" + value + "[label=\"#" + value + "\"];";
    }
  }

  private String newLineWithOffset(String pLine) {
    return  Strings.repeat(" ", offset) + pLine + "\n";
  }

  private final static class Location {

    private final String location;

    private Location(SMGObject pSmgObject, String functionName) {
      location = functionName + "::" + pSmgObject.getLabel();
    }

    public static Location valueOf(MemoryLocation pMemloc) {
      return new Location(pMemloc);
    }

    @Nullable
    public static Location valueOf(SMGObject pObj, String pFunctionName) {
      if (pFunctionName == null) {
        return new Location(pObj);
      } else {
        return new Location(pObj, pFunctionName);
      }
    }

    public static Location valueOf(SMGObject pHeapObject) {
      return new Location(pHeapObject);
    }

    private Location(SMGObject pSmgObject) {
      location = pSmgObject.getLabel();
    }

    private Location(MemoryLocation pMemloc) {
      location = pMemloc.getAsSimpleString();
    }

    @Override
    public boolean equals(Object pObj) {

      if (pObj instanceof Location) {
        return location.equals(((Location) pObj).location);
      }

      return false;
    }

    @Override
    public int hashCode() {
      return location.hashCode();
    }

    @Override
    public String toString() {
      return location;
    }
  }

}