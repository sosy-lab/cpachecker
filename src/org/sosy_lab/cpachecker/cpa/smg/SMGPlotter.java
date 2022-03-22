// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
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
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;

public final class SMGPlotter {
  private static final class SMGObjectNode {
    private final String name;
    private final String definition;
    private static int counter = 0;

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

    private final UnmodifiableCLangSMG smg;

    public SMGNodeDotVisitor(UnmodifiableCLangSMG pSmg) {
      smg = pSmg;
    }

    private SMGObjectNode defaultNode(String label, SMGObject obj) {
      String color = smg.isObjectValid(obj) ? "blue" : "red";
      return new SMGObjectNode(label, defaultDefinition(color, "rectangle", "dashed", obj));
    }

    private String defaultDefinition(
        String pColor, String pShape, String pStyle, SMGObject pObject) {
      return "color="
          + pColor
          + ", shape="
          + pShape
          + ", style="
          + pStyle
          + ", label =\""
          + pObject
          + "\"";
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

  public static void debuggingPlot(
      UnmodifiableCLangSMG pSmg,
      String pId,
      PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> explicitValues)
      throws IOException {
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
  private static int nulls = 0;
  private int offset = 0;

  public SMGPlotter() {} /* utility class */

  public static String convertToValidDot(String original) {
    return CharMatcher.anyOf("[]:").replaceFrom(original, "_");
  }

  public String smgAsDot(
      UnmodifiableCLangSMG smg,
      String name,
      String location,
      PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> explicitValues) {
    StringBuilder sb = new StringBuilder();

    sb.append("digraph gr_").append(name.replace('-', '_')).append("{\n");
    offset += 2;
    sb.append(newLineWithOffset("label = \"" + location.replace("\"", "\\\"") + "\";"));
    sb.append(newLineWithOffset("rankdir=LR;"));

    addStackSubgraph(smg, sb);

    SMGNodeDotVisitor visitor = new SMGNodeDotVisitor(smg);

    for (SMGObject heapObject : smg.getHeapObjects()) {
      if (!objectIndex.containsKey(heapObject)) {
        objectIndex.put(heapObject, heapObject.accept(visitor));
      }
      if (heapObject != SMGNullObject.INSTANCE) {
        sb.append(newLineWithOffset(objectIndex.get(heapObject).getDefinition()));
      }
    }

    addGlobalObjectSubgraph(smg, sb);

    for (SMGValue value : smg.getValues()) {
      if (!value.isZero()) {
        sb.append(newLineWithOffset(smgValueAsDot(value, explicitValues)));
      }
    }

    Set<SMGValue> processed = new HashSet<>();
    for (SMGValue value : smg.getValues()) {
      if (!value.isZero()) {
        for (SMGValue neqValue : smg.getNeqsForValue(value)) {
          if (!processed.contains(neqValue)) {
            sb.append(newLineWithOffset(neqRelationAsDot(value, neqValue, explicitValues)));
          }
        }
        processed.add(value);
      }
    }

    // merge edges with same object and value and print only one edge per source/target.
    Table<SMGObject, SMGValue, Set<SMGEdgeHasValue>> mergedEdges = HashBasedTable.create();
    for (SMGEdgeHasValue edge : smg.getHVEdges()) {
      Set<SMGEdgeHasValue> edges = mergedEdges.get(edge.getObject(), edge.getValue());
      if (edges == null) {
        edges = new LinkedHashSet<>();
        mergedEdges.put(edge.getObject(), edge.getValue(), edges);
      }
      edges.add(edge);
    }
    for (Cell<SMGObject, SMGValue, Set<SMGEdgeHasValue>> entry : mergedEdges.cellSet()) {
      String prefix = "";
      String target = "value_" + entry.getColumnKey().asDotId();
      if (entry.getColumnKey().isZero()) {
        String newNull = newNullLabel();
        prefix = newNull + "[shape=plaintext, label=\"NULL\"];";
        target = newNull;
      }
      List<String> labels = new ArrayList<>();
      for (SMGEdgeHasValue edge : entry.getValue()) {
        labels.add(
            String.format("%db-%db", edge.getOffset(), edge.getOffset() + edge.getSizeInBits()));
      }
      sb.append(
          newLineWithOffset(
              String.format(
                  "%s%s -> %s [label=\"[%s]\"];",
                  prefix,
                  objectIndex.get(entry.getRowKey()).getName(),
                  target,
                  Joiner.on(", ").join(labels))));
    }

    for (SMGEdgePointsTo edge : smg.getPTEdges()) {
      if (!edge.getValue().isZero()) {
        sb.append(newLineWithOffset(smgPTEdgeAsDot(edge)));
      }
    }

    sb.append("}");

    return sb.toString();
  }

  private void addStackSubgraph(UnmodifiableCLangSMG pSmg, StringBuilder pSb) {
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
    pSb.append(
        newLineWithOffset(
            "subgraph cluster_stack_" + pStackFrame.getFunctionDeclaration().getName() + " {"));
    offset += 2;
    pSb.append(newLineWithOffset("fontcolor=blue;"));
    pSb.append(
        newLineWithOffset(
            "label=\"#"
                + pIndex
                + ": "
                + pStackFrame.getFunctionDeclaration().toASTString()
                + "\";"));

    Map<String, SMGRegion> to_print = new LinkedHashMap<>(pStackFrame.getVariables());

    SMGRegion returnObject = pStackFrame.getReturnObject();
    if (returnObject != null) {
      to_print.put(CLangStackFrame.RETVAL_LABEL, returnObject);
    }

    pSb.append(newLineWithOffset(smgScopeFrameAsDot(to_print, String.valueOf(pIndex))));

    offset -= 2;
    pSb.append(newLineWithOffset("}"));
  }

  private String smgScopeFrameAsDot(Map<String, SMGRegion> pNamespace, String structId) {
    List<String> nodes = new ArrayList<>();
    for (Entry<String, SMGRegion> entry : pNamespace.entrySet()) {
      String key = entry.getKey();
      if (key.equals("node")) { // escape "node" for dot
        key = "node1";
      }
      SMGObject obj = entry.getValue();
      nodes.add("<item_" + key + "> " + obj);
      objectIndex.put(obj, new SMGObjectNode("struct" + structId + ":item_" + key));
    }
    return String.format(
        "struct%s [shape=record, height=%.2f, label=\"%s\"];%n",
        structId, .5 * nodes.size(), Joiner.on(" | ").join(nodes));
  }

  private void addGlobalObjectSubgraph(UnmodifiableCLangSMG pSmg, StringBuilder pSb) {
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

  private String smgPTEdgeAsDot(SMGEdgePointsTo pEdge) {
    String str = "value_" + pEdge.getValue().asDotId() + " -> ";
    SMGObjectNode oi = objectIndex.get(pEdge.getObject());
    if (oi != null) {
      str += oi.getName();
    } else {
      str += "\"<invalid object reference>\"";
    }
    return str + "[label=\"+" + pEdge.getOffset() + "b, " + pEdge.getTargetSpecifier() + "\"];";
  }

  private static String smgValueAsDot(
      SMGValue value, PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> explicitValues) {
    String label = "#" + value.asDotId();
    String color = "red";
    if (value instanceof SMGKnownExpValue) {
      label = value.toString();
      color = "green";
    } else if (explicitValues.containsKey(value)) {
      label += " : " + explicitValues.get(value).getAsLong();
      color = "black";
    } else if (value instanceof SMGKnownAddressValue) {
      label += "\\n" + ((SMGKnownAddressValue) value).getObject();
      color = "blue";
    }
    return String.format("value_%s[color=%s label=\"%s\"];", value.asDotId(), color, label);
  }

  private static String neqRelationAsDot(
      SMGValue v1,
      SMGValue v2,
      PersistentBiMap<SMGKnownSymbolicValue, SMGKnownExpValue> explicitValues) {
    String toNodeStr, toNode;
    if (v2.isZero()) {
      final String newLabel = newNullLabel();
      toNode = newLabel;
      toNodeStr = newLabel + "[shape=plaintext, label=\"NULL\", fontcolor=\"red\"];";
    } else {
      toNodeStr = smgValueAsDot(v2, explicitValues);
      toNode = "value_" + v2.asDotId();
    }
    return String.format(
        "%s%n  value_%s -> %s [color=\"red\", fontcolor=\"red\", label=\"neq\"];",
        toNodeStr, v1.asDotId(), toNode);
  }

  private String newLineWithOffset(String pLine) {
    return " ".repeat(offset) + pLine + "\n";
  }
}
