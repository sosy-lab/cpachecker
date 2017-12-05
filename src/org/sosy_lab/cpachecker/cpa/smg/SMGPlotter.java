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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectVisitor;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic.GenericAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.optional.SMGOptionalObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;

public final class SMGPlotter {
  private static final class SMGObjectNode {
    private final String name;
    private final String definition;
    static private int counter = 0;

    public SMGObjectNode(String pType, String pDefinition) {
      name = "node_" + pType + "_" + counter++;
      definition = pDefinition;
    }

    public SMGObjectNode(String pName) {
      name = pName;
      definition = null;
    }

    public String getName() {
      return name;
    }

    public String getDefinition() {
      return name + "[" + definition + "];";
    }
  }

  private static final class SMGNodeDotVisitor implements SMGObjectVisitor<SMGObjectNode> {

    private final CLangSMG smg;


    public SMGNodeDotVisitor(CLangSMG pSmg) {
      smg = pSmg;
    }

    private SMGObjectNode defaultNode(String label, SMGObject obj) {
      String color = smg.isObjectValid(obj) ? "blue" : "red";
      return new SMGObjectNode(label, defaultDefinition(color, "rectangle", "dashed", obj));
    }

    private String defaultDefinition(String pColor, String pShape, String pStyle, SMGObject pObject) {
      return "color=" + pColor + ", shape=" + pShape + ", style=" + pStyle + ", label =\"" + pObject.toString() + "\"";
    }

    @Override
    public SMGObjectNode visit(SMGRegion pRegion) {
      String color;
      String style;
      if (smg.isObjectValid(pRegion)) {
        style = "solid";
        color = "black";
      } else {
        style = "dotted";
        color = "red";
      }
      if (smg.isObjectExternallyAllocated(pRegion)) {
        color = "green";
      }

      return new SMGObjectNode("region", defaultDefinition(color, "rectangle", style, pRegion));
    }

    @Override
    public SMGObjectNode visit(SMGSingleLinkedList sll) {
      return defaultNode("sll", sll);
    }

    @Override
    public SMGObjectNode visit(SMGDoublyLinkedList dll) {
      return defaultNode("dll", dll);
    }

    @Override
    public SMGObjectNode visit(SMGOptionalObject opt) {
      return defaultNode("opt", opt);
    }

    @Override
    public SMGObjectNode visit(GenericAbstraction obj) {
      return defaultNode("abstraction", obj);
    }

    @Override
    public SMGObjectNode visit(SMGNullObject pObject) {
      return new SMGObjectNode("NULL");
    }
  }

  static public void debuggingPlot(CLangSMG pSmg, String pId, Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues) throws IOException {
    PathTemplate exportSMGFilePattern = PathTemplate.ofFormatString("smg-debug-%s.dot");
    pId = pId.replace("\"", "");
    Path outputFile = exportSMGFilePattern.getPath(pId);
    SMGPlotter plotter = new SMGPlotter();

    IO.writeFile(
        outputFile,
        Charset.defaultCharset(),
        plotter.smgAsDot(pSmg, pId, "debug plot", explicitValues));
  }

  private final Map<SMGObject, SMGObjectNode> objectIndex = new HashMap<>();
  static private int nulls = 0;
  private int offset = 0;

  public SMGPlotter() {} /* utility class */

  static public String convertToValidDot(String original) {
    return original.replaceAll("[:]", "_");
  }

  public String smgAsDot(CLangSMG smg, String name, String location, Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph gr_" + name.replace('-', '_') + "{\n");
    offset += 2;
    sb.append(newLineWithOffset("label = \"Location: " + location.replace("\"", "\\\"") + "\";"));

    addStackSubgraph(smg, sb);

    SMGNodeDotVisitor visitor = new SMGNodeDotVisitor(smg);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      if (! objectIndex.containsKey(heapObject)) {
        objectIndex.put(heapObject, heapObject.accept(visitor));
      }
      if (heapObject != SMGNullObject.INSTANCE) {
        sb.append(newLineWithOffset(objectIndex.get(heapObject).getDefinition()));
      }
    }

    addGlobalObjectSubgraph(smg, sb);

    for (int value : smg.getValues()) {
      if (value != SMG.NULL_ADDRESS) {
        sb.append(newLineWithOffset(smgValueAsDot(value, explicitValues)));
      }
    }

    Set<Integer> processed = new HashSet<>();
    for (Integer value : smg.getValues()) {
      if (value != SMG.NULL_ADDRESS) {
        for (Integer neqValue : smg.getNeqsForValue(value)) {
          if (! processed.contains(neqValue)) {
            sb.append(newLineWithOffset(neqRelationAsDot(value, neqValue)));
          }
        }
        processed.add(value);
      }
    }

    for (SMGEdgeHasValue edge: smg.getHVEdges()) {
      sb.append(newLineWithOffset(smgHVEdgeAsDot(edge, smg)));
    }

    for (SMGEdgePointsTo edge : smg.getPTEdges()) {
      if (edge.getValue() != SMG.NULL_ADDRESS) {
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

    Map<String, SMGRegion> to_print = new HashMap<>();
    to_print.putAll(pStackFrame.getVariables());

    SMGRegion returnObject = pStackFrame.getReturnObject();
    if (returnObject != null) {
      to_print.put(CLangStackFrame.RETVAL_LABEL, returnObject);
    }

    pSb.append(newLineWithOffset(smgScopeFrameAsDot(to_print, String.valueOf(pIndex))));

    offset -= 2;
    pSb.append(newLineWithOffset("}"));

  }

  private String smgScopeFrameAsDot(Map<String, SMGRegion> pNamespace, String pStructId) {
    StringBuilder sb = new StringBuilder();
    sb.append("struct" + pStructId + "[shape=record,label=\" ");

    // I sooo wish for Python list comprehension here...
    ArrayList<String> nodes = new ArrayList<>();
    for (Entry<String, SMGRegion> entry : pNamespace.entrySet()) {
      String key = entry.getKey();
      SMGObject obj = entry.getValue();

      if (key.equals("node")) {
        // escape Node1
        key = "node1";
      }

      nodes.add("<item_" + key + "> " + obj.toString());
      objectIndex.put(obj, new SMGObjectNode("struct" + pStructId + ":item_" + key));
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

  private String smgHVEdgeAsDot(SMGEdgeHasValue pEdge, CLangSMG pSMG) {
    if (pEdge.getValue() == 0) {
      String newNull = newNullLabel();
      return newNull + "[shape=plaintext, label=\"NULL\"];" + objectIndex.get(pEdge.getObject())
          .getName() + " -> " + newNull + "[label=\"[" + pEdge.getOffset() + "b-" + (pEdge
          .getOffset() + pEdge.getSizeInBits(pSMG.getMachineModel())) + "b]\"];";
    } else {
      return objectIndex.get(pEdge.getObject()).getName() + " -> value_" + pEdge.getValue() +
          "[label=\"[" + pEdge.getOffset() + "b-" + (pEdge.getOffset() + pEdge.getSizeInBits
          (pSMG.getMachineModel())) + "b]\"];";
    }
  }

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge) {
    return "value_" + pEdge.getValue() + " -> " + objectIndex.get(pEdge.getObject()).getName() + "[label=\"+" + pEdge.getOffset() + "b, " + pEdge.getTargetSpecifier() + "\"];";
  }

  private static String smgValueAsDot(int value, Map<SMGKnownSymValue, SMGKnownExpValue> explicitValues) {
    String explicitValue = "";
    SMGKnownSymValue symValue =  SMGKnownSymValue.valueOf(value);
    if (explicitValues.containsKey(symValue)) {
      explicitValue = " : " + String.valueOf(explicitValues.get(symValue).getAsLong());
    }
    return "value_" + value + "[label=\"#" + value + explicitValue +  "\"];";
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
