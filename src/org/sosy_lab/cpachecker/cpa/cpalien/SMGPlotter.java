/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;


public final class SMGPlotter {
  private final HashMap <SMGObject, String> objectIndex = new HashMap<>();
  private int offset = 0;

  public SMGPlotter() {} /* utility class */

  public String smgAsDot(CLangSMG smg, String name) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph " + name.replace('-', '_') + "{\n");
    offset += 2;

    addStackSubgraph(smg, sb);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      //if (heapObject.notNull()){
        sb.append(newLineWithOffset(smgObjectAsDot(heapObject)));
        objectIndex.put(heapObject, heapObject.getLabel());
      //}
    }

    addGlobalObjectSubgraph(smg, sb);

    for (int value : smg.getValues()) {
      sb.append(newLineWithOffset(smgValueAsDot(value) ));
    }

    for (SMGEdgeHasValue edge: smg.getHVEdges()) {
      sb.append(newLineWithOffset(smgHVEdgeAsDot(edge)));
    }

    for (SMGEdgePointsTo edge: smg.getPTEdges()) {
      sb.append(newLineWithOffset(smgPTEdgeAsDot(edge)));
    }

    sb.append("}");

    return sb.toString();
  }

  private void addStackSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_stack {"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Stack\";"));

    int i = 0;
    for (CLangStackFrame stack_item : pSmg.getStackFrames() ) {
      addStackItemSubgraph(stack_item, pSb, i);
      i++;
    }
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private void addStackItemSubgraph(CLangStackFrame pStackFrame, StringBuilder pSb, int pIndex) {
    pSb.append(newLineWithOffset("subgraph cluster_stack_" + pStackFrame.getFunctionDeclaration().getName() + "{"));
    offset += 2;
    pSb.append(newLineWithOffset("fontcolor=blue;"));
    pSb.append(newLineWithOffset("label=\"" + pStackFrame.getFunctionDeclaration().toASTString() + "\";"));

    pSb.append(newLineWithOffset(smgScopeFrameAsDot(pStackFrame.getVariables(), String.valueOf(pIndex))));

    offset -= 2;
    pSb.append(newLineWithOffset("}"));

  }

  private String smgScopeFrameAsDot(Map<String, SMGObject> pNamespace, String pStructId) {
    StringBuilder sb = new StringBuilder();
    sb.append("struct" + pStructId + "[shape=record,label=\" ");

    // I sooo wish for Python list comprehension here...
    ArrayList<String> nodes = new ArrayList<>();
    for (String key : pNamespace.keySet()) {
      SMGObject obj = pNamespace.get(key);
      nodes.add("<" + key + "> " + obj.toString());
      objectIndex.put(obj, "struct" + pStructId + ":" + key);
     }
    sb.append(Joiner.on(" | ").join(nodes));
    sb.append("\"];\n");
    return sb.toString();
  }

  private void addGlobalObjectSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_global{"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Global objects\";"));
    pSb.append(newLineWithOffset(smgScopeFrameAsDot(pSmg.getGlobalObjects(), "global")));
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private String smgHVEdgeAsDot(SMGEdgeHasValue pEdge) {
    return objectIndex.get(pEdge.getObject()) + " -> value_" + pEdge.getValue() + "[label=\"[" + pEdge.getOffset() + "]\"];";
  }

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge) {
    return "value_" + pEdge.getValue() + " -> " + objectIndex.get(pEdge.getObject()) + "[label=\"+" + pEdge.getOffset() + "b\"];";
  }

  private static String smgObjectAsDot(SMGObject pObject) {
    return pObject.getLabel() + " [ shape=rectangle, label = \"" + pObject.toString() + "\"];";
  }

  private static String smgValueAsDot(int value) {
    return "value_" + value + "[label=\"#" + value + "\"];";
  }

  private String newLineWithOffset(String pLine) {
    return  Strings.repeat(" ", offset) + pLine + "\n";
  }
}