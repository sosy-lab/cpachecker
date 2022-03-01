// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport.formatter;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.ElementType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class WitnessToDotFormatter extends WitnessToOutputFormatter<String> {

  private Map<String, Collection<String>> nodesToLabel;
  private Map<String, String> nodesToColor;

  public WitnessToDotFormatter(Witness pWitness) {
    super(pWitness);
  }

  @Override
  protected void initialize(Appendable pTarget) throws IOException {
    nodesToLabel = new LinkedHashMap<>();
    nodesToColor = new LinkedHashMap<>();
    // start dot-graph
    pTarget.append("digraph WITNESS {\n");
  }

  @Override
  protected void finish(Appendable pTarget) throws IOException {

    // we delay nodes, because their label is build and extended during the witness traversal.
    for (Entry<String, Collection<String>> entry : nodesToLabel.entrySet()) {
      String nodeId = entry.getKey();
      String color = nodesToColor.getOrDefault(nodeId, "");
      pTarget
          .append(nodeId)
          .append(" [label=\"")
          .append(Joiner.on("\\n").join(entry.getValue()))
          .append("\"")
          .append(color)
          .append("];\n");
    }

    // finish dot-graph
    pTarget.append("\n}");
  }

  private static String eq(Object o1, Object o2) {
    // escape more complex labels
    String o2str = o2.toString();
    if (o2str.contains(" ")) {
      o2str = "\\\"" + o2str + "\\\"";
    }
    return String.format("%s=%s", o1, o2str);
  }

  @Override
  protected String createNewNode(String pNodeId, Appendable pTarget) throws IOException {
    final String nodeId = Integer.toString(nodesToLabel.size());
    List<String> labels = new ArrayList<>();
    // if (witness.getWitnessOptions().exportNodeLabel()) {
    labels.add(pNodeId);
    // }
    for (NodeFlag f : witness.getNodeFlags().get(pNodeId)) {
      labels.add(eq(f.key, "true"));
      nodesToColor.put(nodeId, nodesToColor.getOrDefault(nodeId, "") + " " + getColorForNode(f));
    }
    for (Property violation : witness.getViolatedProperties().get(pNodeId)) {
      labels.add(eq(KeyDef.VIOLATEDPROPERTY, violation));
    }
    if (witness.hasQuasiInvariant(pNodeId)) {
      ExpressionTree<Object> tree = witness.getQuasiInvariant(pNodeId);
      labels.add(eq(KeyDef.INVARIANT, tree));
    }
    nodesToLabel.put(nodeId, labels);
    return nodeId;
  }

  private static String getColorForNode(NodeFlag f) {
    switch (f) {
      case ISFRONTIER:
        return "color=orange";
      case ISVIOLATION:
        return "color=red";
      case ISENTRY:
        return "color=green";
      case ISSINKNODE:
        return "color=blue";
      case ISCYCLEHEAD:
        return "shape=doublecircle";
      default:
        return "";
    }
  }

  @Override
  protected void createNewEdge(
      Edge pEdge, String pSourceNode, String pTargetNode, Appendable pTarget) throws IOException {
    List<String> labels = new ArrayList<>();
    String color = "";
    for (Map.Entry<KeyDef, String> entry : pEdge.getLabel().getMapping().entrySet()) {
      KeyDef keyDef = entry.getKey();
      String value = entry.getValue();
      if (keyDef.keyFor.equals(ElementType.EDGE)) {
        labels.add(eq(keyDef, value));
        if (KeyDef.THREADID.equals(keyDef)) {
          color = "colorscheme=set19 color=" + value; // trick to get different colors
        }
      } else if (keyDef.keyFor.equals(ElementType.NODE)) {
        nodesToLabel.get(pTargetNode).add(eq(keyDef, value));
      }
    }
    pTarget
        .append(pSourceNode)
        .append(" -> ")
        .append(pTargetNode)
        .append(" [label=\"")
        .append(Joiner.on("\\n").join(labels))
        .append("\" ")
        .append(color)
        .append("];\n");
  }

  @Override
  protected void addInvariantsData(
      String pNodeId, ExpressionTree<Object> pTree, @Nullable String pScope, Appendable pTarget) {
    nodesToLabel.get(pNodeId).add(eq(KeyDef.INVARIANT, pTree));
    if (!isNullOrEmpty(pScope) && !pTree.equals(ExpressionTrees.getFalse())) {
      nodesToLabel.get(pNodeId).add(eq(KeyDef.INVARIANTSCOPE, pScope));
    }
  }
}
