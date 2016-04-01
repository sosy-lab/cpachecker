/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.export;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for generating a GraphML file from a CFA.
 */
public final class GraphMLBuilder {

  private GraphMLBuilder() {
    /* utility class */
  }

  private static int edgeCounter = 0;

  private static final String MAIN_GRAPH = "___Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');

  // After this many characters the node shape changes to box
  private static final int NODE_SHAPE_CHANGE_CHAR_LIMIT = 10;

  private static final Function<CFANode, String> DEFAULT_NODE_FORMATTER =
      new Function<CFANode, String>() {
        @Override
        public String apply(final CFANode pNode) {
          return "N" + pNode.getNodeNumber() + "\n" + pNode.getReversePostorderId();
        }
      };

  public static void generateGraphML(final Appendable pSB, final CFA pCFA) throws IOException {
    generateGraphML(pSB, pCFA, DEFAULT_NODE_FORMATTER);
  }

  public static void generateGraphML(
      final Appendable pSB, final CFA pCFA, final Function<CFANode, String> pFormatNodeLabel)
      throws IOException {

    GraphMLGenerator graphMLGenerator = new GraphMLGenerator(pCFA, pFormatNodeLabel);
    CFATraversal.dfs().traverseOnce(pCFA.getMainFunction(), graphMLGenerator);

    appendDocHeader(pSB);

    JOINER_ON_NEWLINE.appendTo(pSB, graphMLGenerator.nodes);

    for (FunctionEntryNode node : pCFA.getAllFunctionHeads()) {
      JOINER_ON_NEWLINE.appendTo(pSB, graphMLGenerator.edges.get(node.getFunctionName()));
    }

    JOINER_ON_NEWLINE.appendTo(pSB, graphMLGenerator.edges.get(MAIN_GRAPH));

    finish(pSB);
  }

  private static void appendDocHeader(final Appendable pSB) throws IOException {
    pSB.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
    pSB.append(
        "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
            + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
            + "http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd\">\n");
    pSB.append("  <key for=\"node\" id=\"d0\" yfiles.type=\"nodegraphics\"/>\n");
    pSB.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"node\" id=\"d1\"/>\n");
    pSB.append("  <key for=\"edge\" id=\"d2\" yfiles.type=\"edgegraphics\"/>\n");
    pSB.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"edge\" id=\"d3\"/>\n");
    pSB.append("  <key for=\"graphml\" id=\"d4\" yfiles.type=\"resources\"/>\n\n");

    pSB.append("  <graph edgedefault=\"directed\" id=\"G\">\n");
  }

  private static void finish(final Appendable pSB) throws IOException {
    pSB.append("  </graph>\n");
    pSB.append("  <data key=\"d4\">\n");
    pSB.append("    <y:Resources/>\n");
    pSB.append("  </data>\n");
    pSB.append("</graphml>\n");
  }

  private static class GraphMLGenerator implements CFATraversal.CFAVisitor {

    private final List<String> nodes = new ArrayList<>();

    // edges for each function
    private final ListMultimap<String, String> edges = ArrayListMultimap.create();

    private final Optional<ImmutableSet<CFANode>> loopHeads;
    private final Function<CFANode, String> formatNodeLabel;

    public GraphMLGenerator(final CFA pCFA, final Function<CFANode, String> pFormatNodeLabel) {
      loopHeads = pCFA.getAllLoopHeads();
      formatNodeLabel = pFormatNodeLabel;
    }

    @Override
    public TraversalProcess visitEdge(final CFAEdge pEdge) {
      CFANode predecessor = pEdge.getPredecessor();
      List<String> graph;
      if (pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge
          || pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        graph = edges.get(MAIN_GRAPH);
      } else {
        graph = edges.get(predecessor.getFunctionName());
      }
      graph.add(formatEdge(pEdge));

      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(final CFANode pNode) {
      nodes.add(formatNode(pNode, loopHeads, formatNodeLabel));

      return TraversalProcess.CONTINUE;
    }

    private static String formatEdge(final CFAEdge pEdge) {
      StringBuilder builder = new StringBuilder();
      edgeCounter++;

      builder
          .append("    <edge id=\"e")
          .append(edgeCounter)
          .append("\" source=\"n")
          .append(pEdge.getPredecessor().getNodeNumber())
          .append("\" target=\"n")
          .append(pEdge.getSuccessor().getNodeNumber())
          .append("\">\n");
      builder.append("      <data key=\"d2\">\n");
      builder.append("        <y:PolyLineEdge>\n");

      if (pEdge instanceof FunctionSummaryEdge) {
        builder.append(
            "          <y:LineStyle color=\"#000000\" type=\"dotted\" width=\"1.0\"/>\n");
      } else {
        builder.append("          <y:LineStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>\n");
      }
      builder.append("          <y:Arrows source=\"none\" target=\"delta\"/>\n");

      builder
          .append("          <y:EdgeLabel alignment=\"center\" distance=\"2.0\" ")
          .append("fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" ")
          .append("hasBackgroundColor=\"false\" hasLineColor=\"false\" modelName=\"six_pos\" ")
          .append("modelPosition=\"tail\" preferredPlacement=\"anywhere\" ratio=\"0.5\" ")
          .append("textColor=\"#000000\" visible=\"true\">")
          .append(
              pEdge
                  .getDescription()
                  .replaceAll("\n", " ")
                  .replace('"', '\'')
                  .replaceAll("&", "&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;"))
          .append("</y:EdgeLabel>\n");
      builder.append("          <y:BendStyle smoothed=\"true\"/>\n");

      builder.append("        </y:PolyLineEdge>\n");
      builder.append("      </data>\n");
      builder.append("      <data key=\"d3\"/>\n");
      builder.append("    </edge>\n\n");

      return builder.toString();
    }
  }

  static String formatNode(final CFANode pNode, final Optional<ImmutableSet<CFANode>> pLoopHeads) {
    return formatNode(pNode, pLoopHeads, DEFAULT_NODE_FORMATTER);
  }

  private static String formatNode(
      final CFANode pNode,
      final Optional<ImmutableSet<CFANode>> pLoopHeads,
      Function<CFANode, String> pFormatNodeLabel) {

    final String shape;
    String borderWeight = "1.0";
    final StringBuilder builder = new StringBuilder();

    String nodeAnnotation = pFormatNodeLabel.apply(pNode);
    nodeAnnotation = nodeAnnotation != null ? nodeAnnotation : "";

    if (nodeAnnotation.length() > NODE_SHAPE_CHANGE_CHAR_LIMIT) {
      shape = "roundrectangle";
    } else {
      if (pLoopHeads.isPresent() && pLoopHeads.get().contains(pNode)) {
        shape = "ellipse";
        borderWeight = "3.0";
      } else if (pNode.isLoopStart()) {
        shape = "octagon";
      } else if (pNode.getNumLeavingEdges() > 0
          && pNode.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge) {
        shape = "diamond";
      } else {
        shape = "ellipse";
      }
    }

    builder.append("    <node id=\"n").append(pNode.getNodeNumber()).append("\">\n");
    builder.append("      <data key=\"d0\">\n");
    builder.append("        <y:ShapeNode>\n");
    builder.append("          <y:Geometry height=\"50.0\" width=\"50.0\" x=\"0.0\" y=\"0.0\"/>\n");
    builder.append("          <y:Fill color=\"#FFFFFF\" transparent=\"false\"/>\n");
    builder
        .append("          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"")
        .append(borderWeight)
        .append("\"/>\n");
    builder
        .append("          <y:NodeLabel alignment=\"center\" autoSizePolicy=\"content\" ")
        .append("fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" ")
        .append("hasBackgroundColor=\"false\" hasLineColor=\"false\" modelName=\"internal\" ")
        .append("modelPosition=\"c\" textColor=\"#000000\" visible=\"true\">")
        .append(nodeAnnotation)
        .append("</y:NodeLabel>\n");
    builder.append("          <y:Shape type=\"").append(shape).append("\"/>\n");
    builder.append("        </y:ShapeNode>\n");
    builder.append("      </data>\n");
    builder.append("      <data key=\"d1\"/>\n");
    builder.append("    </node>\n\n");

    return builder.toString();
  }
}
