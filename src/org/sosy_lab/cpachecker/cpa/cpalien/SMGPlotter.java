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
package org.sosy_lab.cpachecker.cpa.cpalien2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Files;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;


public final class SMGPlotter {
  static final public void debuggingPlot(CLangSMG pSmg, String pId) throws IOException {
    File exportSMGFilePattern = new File("smg-debug-%s.dot");
    pId = pId.replace("\"", "");
    File outputFile = new File(String.format(exportSMGFilePattern.getAbsolutePath(), pId));
    SMGPlotter plotter = new SMGPlotter();

    Files.writeFile(outputFile, plotter.smgAsDot(pSmg, pId, "debug plot"));
  }

  private final HashMap <SMGObject, String> objectIndex = new HashMap<>();
  static private int nulls = 0;
  private int offset = 0;

  public SMGPlotter() {} /* utility class */

  private String convertToValidDot(String original) {
    return original.replaceAll("[:]", "_");
  }

  public String smgAsDot(CLangSMG smg, String name, String location) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph gr_" + name.replace('-', '_') + "{\n");
    offset += 2;
    sb.append(newLineWithOffset("label = \"Location: " + location.replace("\"", "\\\"") + "\";"));

    addStackSubgraph(smg, sb);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      if (heapObject.notNull()) {
        sb.append(newLineWithOffset(smgObjectAsDot(heapObject, smg.isObjectValid(heapObject))));
      }
      objectIndex.put(heapObject, convertToValidDot(heapObject.getLabel()));
    }

    addGlobalObjectSubgraph(smg, sb);

    for (int value : smg.getValues()) {
      if (value != smg.getNullValue()) {
        sb.append(newLineWithOffset(smgValueAsDot(value)));
      }
    }

    Set<Integer> processed = new HashSet<>();
    for (Integer value : smg.getValues()) {
      if (value != smg.getNullValue()) {
        for (Integer neqValue : smg.getNeqsForValue(value)) {
          if (! processed.contains(neqValue)) {
            sb.append(newLineWithOffset(neqRelationAsDot(value, neqValue)));
          }
        }
        processed.add(value);
      }
    }

    for (SMGEdgeHasValue edge: smg.getHVEdges()) {
      sb.append(newLineWithOffset(smgHVEdgeAsDot(edge)));
    }

    for (SMGEdgePointsTo edge: smg.getPTEdges().values()) {
      if (edge.getValue() != smg.getNullValue()) {
        sb.append(newLineWithOffset(smgPTEdgeAsDot(edge)));
      }
    }

    sb.append("}");

    return sb.toString();
  }

  private void addStackSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    pSb.append(newLineWithOffset("subgraph cluster_stack {"));
    offset += 2;
    pSb.append(newLineWithOffset("label=\"Stack\";"));

    int i = pSmg.getStackFrames().size();
    for (CLangStackFrame stack_item : pSmg.getStackFrames()) {
      addStackItemSubgraph(stack_item, pSb, i);
      i--;
    }
    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private void addStackItemSubgraph(CLangStackFrame pStackFrame, StringBuilder pSb, int pIndex) {
    pSb.append(newLineWithOffset("subgraph cluster_stack_" + pStackFrame.getFunctionDeclaration().getName() + "{"));
    offset += 2;
    pSb.append(newLineWithOffset("fontcolor=blue;"));
    pSb.append(newLineWithOffset("label=\"#" + pIndex + ": " + pStackFrame.getFunctionDeclaration().toASTString() + "\";"));

    HashMap<String, SMGObject> to_print = new HashMap<>();
    to_print.putAll(pStackFrame.getVariables());

    SMGObject returnObject = pStackFrame.getReturnObject();
    if (returnObject != null) {
      to_print.put(CLangStackFrame.RETVAL_LABEL, returnObject);
    }

    pSb.append(newLineWithOffset(smgScopeFrameAsDot(to_print, String.valueOf(pIndex))));

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

      if (key.equals("node")) {
        // escape Node1
        key = "node1";
      }

      nodes.add("<" + key + "> " + obj.toString());
      objectIndex.put(obj, "struct" + pStructId + ":" + key);
     }
    sb.append(Joiner.on(" | ").join(nodes));
    sb.append("\"];\n");
    return sb.toString();
  }

  private void addGlobalObjectSubgraph(CLangSMG pSmg, StringBuilder pSb) {
    if (pSmg.getGlobalObjects().size() > 0) {
      pSb.append(newLineWithOffset("subgraph cluster_global{"));
      offset += 2;
      pSb.append(newLineWithOffset("label=\"Global objects\";"));
      pSb.append(newLineWithOffset(smgScopeFrameAsDot(pSmg.getGlobalObjects(), "global")));
      offset -= 2;
      pSb.append(newLineWithOffset("}"));
    }
  }

  private static String newNullLabel() {
    SMGPlotter.nulls += 1;
    return "value_null_" + SMGPlotter.nulls;
  }

  private String smgHVEdgeAsDot(SMGEdgeHasValue pEdge) {
    if (pEdge.getValue() == 0) {
      String newNull = newNullLabel();
      return newNull + "[shape=plaintext, label=\"NULL\"];" + objectIndex.get(pEdge.getObject()) + " -> " + newNull + "[label=\"[" + pEdge.getOffset() + "]\"];";
    } else {
      return objectIndex.get(pEdge.getObject()) + " -> value_" + pEdge.getValue() + "[label=\"[" + pEdge.getOffset() + "]\"];";
    }
  }

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge) {
    return "value_" + pEdge.getValue() + " -> " + convertToValidDot(objectIndex.get(pEdge.getObject())) + "[label=\"+" + pEdge.getOffset() + "b\"];";
  }

  private String smgObjectAsDot(SMGObject pObject, boolean pValidity) {
    String shape;
    String color;
    if (pValidity) {
      shape="rectangle"; color="black";
    } else {
      shape="doubleoctagon"; color="red";
    }
    return this.convertToValidDot(pObject.getLabel()) + " [ color=" + color + ", shape=" + shape + ", label = \"" + pObject.toString() + "\"];";
  }

  private static String smgValueAsDot(int value) {
    return "value_" + value + "[label=\"#" + value + "\"];";
  }

  private static String neqRelationAsDot(Integer v1, Integer v2) {
    String targetNode;
    String returnString = "";
    if (v2.equals(0)) {
      targetNode = newNullLabel();
      returnString = targetNode + "[shape=plaintext, label=\"NULL\", fontcolor=\"red\"];\n";
    } else {
      targetNode = "value_" + v2;
    }
    return returnString + "value_" + v1 + " -> " + targetNode + "[color=\"red\", fontcolor=\"red\", label=\"neq\"]";
  }

  private String newLineWithOffset(String pLine) {
    return  Strings.repeat(" ", offset) + pLine + "\n";
  }
}