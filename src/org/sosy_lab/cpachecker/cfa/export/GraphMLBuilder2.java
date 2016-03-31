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

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.CFAUtils.successorsOf;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates one GraphML file per function for the report.
 */
public class GraphMLBuilder2 {

  private static int edgeCounter = 0;

  private GraphMLBuilder2() {
    /* utility class */
  }

  /**
   * Output the CFA as GraphML file
   *
   * @param pCFA    The CFA
   * @param pOutDir The directory to place the files in
   * @throws IOException In case of an IO problem
   */
  public static void writeReport(final CFA pCFA, final Path pOutDir) throws IOException {
    GraphMLViewBuilder grapher = new GraphMLViewBuilder(pCFA);
    CFAVisitor visitor = new NodeCollectingCFAVisitor(new CompositeCFAVisitor(grapher));
    for (FunctionEntryNode entryNode : pCFA.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverse(entryNode, visitor);
      grapher.writeFunctionFile(entryNode.getFunctionName(), pOutDir);
    }
  }

  private static String getEdgeText(final CFAEdge pEdge) {
    return pEdge
        .getDescription()
        .replaceAll("\\Q\\\"\\E", "\\ \"")
        .replaceAll("\\\"", "\\\\\\\"")
        .replaceAll("\n", " ")
        .replaceAll("\\s+", " ")
        .replaceAll(" ;", ";");
  }

  /**
   * Output GraphML files and meta information about virtual and combined edges
   */
  private static class GraphMLViewBuilder extends DefaultCFAVisitor {

    //global state for all functions
    private int virtFuncCallNodeIdeCounter = 100000;

    // local state per function
    private final Set<CFANode> nodes = Sets.newLinkedHashSet();
    private final List<CFAEdge> edges = Lists.newArrayList();
    private final List<List<CFAEdge>> comboEdges = Lists.newArrayList();

    private List<CFAEdge> currentComboEdge = null;

    private final Optional<ImmutableSet<CFANode>> loopHeads;

    private GraphMLViewBuilder(final CFA pCFA) {
      loopHeads = pCFA.getAllLoopHeads();
    }

    @Override
    public TraversalProcess visitEdge(final CFAEdge pEdge) {
      CFANode predecessor = pEdge.getPredecessor();

      // check if it qualifies for a comboEdge
      if (predecessor.isLoopStart()
          || predecessor.getNumEnteringEdges() != 1
          || predecessor.getNumLeavingEdges() != 1
          || (currentComboEdge != null
              && !predecessor.equals(
                  currentComboEdge.get(currentComboEdge.size() - 1).getSuccessor()))
          || pEdge.getEdgeType() == CFAEdgeType.CallToReturnEdge
          || pEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        // no it does not
        edges.add(pEdge);
        currentComboEdge = null;

        // nodes are only added if they are not hidden by a combo edge
        nodes.add(predecessor);
        nodes.add(pEdge.getSuccessor());
      } else {
        // add combo edge
        if (currentComboEdge == null) {
          currentComboEdge = Lists.newArrayList();
          comboEdges.add(currentComboEdge);
        }
        currentComboEdge.add(pEdge);
      }

      return TraversalProcess.CONTINUE;
    }

    void writeFunctionFile(final String pFunctionName, final Path pOutDir) throws IOException {
      try (Writer out =
              pOutDir
                  .resolve("cfa__" + pFunctionName + ".graphml")
                  .asCharSink(StandardCharsets.UTF_8)
                  .openBufferedStream()) {
        out.write(writeXMLHeader());

        StringBuilder builder = new StringBuilder();
        // write combo edges
        for (final List<CFAEdge> combo : comboEdges) {
          if (combo.size() == 1) {
            edges.add(combo.get(0));
            nodes.add(combo.get(0).getPredecessor());
            nodes.add(combo.get(0).getSuccessor());
          } else {
            builder.append(comboToGraphML(combo));

            CFAEdge first = combo.get(0);
            CFAEdge last = combo.get(combo.size() - 1);

            builder.append(
                createEdge(
                    first.getPredecessor().getNodeNumber(), last.getSuccessor().getNodeNumber()));
          }
        }

        // write nodes
        for (final CFANode node : nodes) {
          out.write(GraphMLBuilder.formatNode(node, loopHeads));
          out.write("\n");
        }

        out.write(builder.toString());

        // write edges
        for (final CFAEdge edge : edges) {
          out.write(edgeToGraphML(edge));
        }

        out.write(writeXMLFooter());

        nodes.clear();
        edges.clear();
        comboEdges.clear();
      }
    }

    private String comboToGraphML(final List<CFAEdge> pComboEdges) {
      final CFAEdge first = pComboEdges.get(0);
      StringBuilder sb = new StringBuilder();
      final int firstNo = first.getPredecessor().getNodeNumber();

      if (pComboEdges.size() > 20) {
        final CFAEdge last = pComboEdges.get(pComboEdges.size() - 1);
        final int lastNo = last.getPredecessor().getNodeNumber();
        sb.append(
            createNode(
                firstNo,
                "\"Long linear chain of edges between nodes "
                    + firstNo
                    + ""
                    + " and "
                    + lastNo
                    + "\""));
      } else {
        StringBuilder label = new StringBuilder();
        for (final CFAEdge edge : pComboEdges) {
          label.append(edge.getPredecessor().getNodeNumber()).append("    ");
          label
              .append(
                  getEdgeText(edge)
                      .replaceAll("\\|", "&#124;")
                      .replaceAll("&", "&amp;")
                      .replaceAll("<", "&lt;")
                      .replaceAll(">", "&gt;"))
              .append("\n");
        }
        sb.append(createNode(firstNo, label.toString()));
      }

      return sb.toString();
    }

    private String edgeToGraphML(final CFAEdge pEdge) {
      if (pEdge.getEdgeType() == CFAEdgeType.CallToReturnEdge) {
        // create the function node
        final CFANode functionEntryNode =
            getOnlyElement(successorsOf(pEdge.getPredecessor()).filter(FunctionEntryNode.class));
        final String calledFunction = functionEntryNode.getFunctionName();
        virtFuncCallNodeIdeCounter++;
        String ret = createNode(virtFuncCallNodeIdeCounter, calledFunction);
        final int from = pEdge.getPredecessor().getNodeNumber();
        ret += createEdge(from, virtFuncCallNodeIdeCounter, getEdgeText(pEdge));

        final int to = pEdge.getSuccessor().getNodeNumber();
        ret += createEdge(virtFuncCallNodeIdeCounter, to);

        return ret;
      }

      return createEdge(
          pEdge.getPredecessor().getNodeNumber(),
          pEdge.getSuccessor().getNodeNumber(),
          getEdgeText(pEdge));
    }

    private String writeXMLHeader() {
      final StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      sb.append(
          "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
              + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
              + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
              + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
              + "http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd\">\n");
      sb.append("  <key for=\"node\" id=\"d0\" yfiles.type=\"nodegraphics\"/>\n");
      sb.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"node\" id=\"d1\"/>\n");
      sb.append("  <key for=\"edge\" id=\"d2\" yfiles.type=\"edgegraphics\"/>\n");
      sb.append("  <key attr.name=\"description\" attr.type=\"string\" for=\"edge\" id=\"d3\"/>\n");
      sb.append("  <key for=\"graphml\" id=\"d4\" yfiles.type=\"resources\"/>\n\n");
      sb.append("  <graph edgedefault=\"directed\" id=\"G\">\n");
      return sb.toString();
    }

    private String writeXMLFooter() {
      final StringBuilder sb = new StringBuilder();
      sb.append("  </graph>\n");
      sb.append("  <data key=\"d4\">\n");
      sb.append("    <y:Resources/>\n");
      sb.append("  </data>\n");
      sb.append("</graphml>\n");
      return sb.toString();
    }

    private String createEdge(final int pPredecessorNodeNumber, final int pSuccessNodeNumber) {
      return createEdge(pPredecessorNodeNumber, pSuccessNodeNumber, "");
    }

    private String createEdge(
        final int pPredecessorNodeNumber, final int pSuccessorNodeNumber, final String pText) {
      StringBuilder builder = new StringBuilder();
      edgeCounter++;

      builder
          .append("    <edge id=\"e")
          .append(edgeCounter)
          .append("\" source=\"n")
          .append(pPredecessorNodeNumber)
          .append("\" target=\"n")
          .append(pSuccessorNodeNumber)
          .append("\">\n");
      builder.append("      <data key=\"d2\">\n");
      builder.append("        <y:PolyLineEdge>\n");
      builder.append("          <y:LineStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>\n");
      builder
          .append("          <y:EdgeLabel alignment=\"center\" distance=\"2.0\" ")
          .append("fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" ")
          .append("hasBackgroundColor=\"false\" hasLineColor=\"false\" modelName=\"six_pos\" ")
          .append("modelPosition=\"tail\" preferredPlacement=\"anywhere\" ratio=\"0.5\" ")
          .append("textColor=\"#000000\" visible=\"true\">")
          .append(
              pText
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

    private String createNode(final int pNodeId, final String pNodeLabel) {
      final StringBuilder builder = new StringBuilder();

      builder.append("    <node id=\"n").append(pNodeId).append("\">\n");
      builder.append("      <data key=\"d0\">\n");
      builder.append("        <y:ShapeNode>\n");
      builder.append(
          "          <y:Geometry height=\"50.0\" width=\"300.0\" x=\"0.0\" y=\"0.0\"/>\n");
      builder.append("          <y:Fill color=\"#FFFFFF\" transparent=\"false\"/>\n");
      builder.append("          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"0.0\"/>\n");
      builder
          .append("          <y:NodeLabel alignment=\"center\" autoSizePolicy=\"content\" ")
          .append("fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" ")
          .append("hasBackgroundColor=\"false\" hasLineColor=\"false\" modelName=\"internal\" ")
          .append("modelPosition=\"c\" textColor=\"#000000\" visible=\"true\">")
          .append(pNodeLabel)
          .append("</y:NodeLabel>\n");
      builder.append("          <y:Shape type=\"roundrectangle\"/>\n");
      builder.append("        </y:ShapeNode>\n");
      builder.append("      </data>\n");
      builder.append("      <data key=\"d1\"/>\n");
      builder.append("    </node>\n\n");

      return builder.toString();
    }
  }
}
