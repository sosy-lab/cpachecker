// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Strings.nullToEmpty;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

/** Class for generating a DOT file from a CFA. */
public final class DOTBuilder {

  private DOTBuilder() {
    /* utility class */
  }

  private static final String MAIN_GRAPH = "____Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');

  // After this many characters the node shape changes to box.
  private static final int NODE_SHAPE_CHANGE_CHAR_LIMIT = 10;

  private static String formatNode(CFANode node) {
    return "N" + node.getNodeNumber() + "\\n" + node.getReversePostorderId();
  }

  public static void generateDOT(Appendable sb, CFA cfa) throws IOException {
    generateDOT(sb, cfa, DOTBuilder::formatNode);
  }

  public static void generateDOT(Appendable sb, CFA cfa, Function<CFANode, String> formatNodeLabel)
      throws IOException {
    DotGenerator dotGenerator = new DotGenerator(cfa, formatNodeLabel);
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), dotGenerator);

    sb.append("digraph " + "CFA" + " {\n");

    JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.nodes);
    sb.append('\n');

    // define the graphic representation for all subsequent nodes
    sb.append("node [shape=\"circle\"]\n");

    for (FunctionEntryNode fnode : cfa.getAllFunctionHeads()) {
      // If Array belongs to functionCall in Parameter, replace [].
      // If Name Contains '.' replace with '_'
      sb.append(
          "subgraph cluster_"
              + fnode.getFunctionName().replace("[", "").replace("]", "_array").replace(".", "_")
              + " {\n");
      sb.append("label=\"" + fnode.getFunctionName() + "()\"\n");
      JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.edges.get(fnode.getFunctionName()));
      sb.append("}\n");
    }

    JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.edges.get(MAIN_GRAPH));
    sb.append("}");
  }

  private static class DotGenerator implements CFATraversal.CFAVisitor {

    private final List<String> nodes = new ArrayList<>();

    // edges for each function
    private final ListMultimap<String, String> edges = ArrayListMultimap.create();

    private final Optional<ImmutableSet<CFANode>> loopHeads;
    private final Function<CFANode, String> formatNodeLabel;

    public DotGenerator(CFA cfa, Function<CFANode, String> pFormatNodeLabel) {
      loopHeads = cfa.getAllLoopHeads();
      formatNodeLabel = pFormatNodeLabel;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      CFANode predecessor = edge.getPredecessor();
      List<String> graph;
      if ((edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
          || edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        graph = edges.get(MAIN_GRAPH);
      } else {
        graph = edges.get(predecessor.getFunctionName());
      }
      graph.add(formatEdge(edge));

      return CFATraversal.TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode node) {
      nodes.add(formatNode(node, loopHeads, formatNodeLabel));

      return CFATraversal.TraversalProcess.CONTINUE;
    }

    private static String formatEdge(CFAEdge edge) {
      StringBuilder sb = new StringBuilder();
      sb.append(edge.getPredecessor().getNodeNumber());
      sb.append(" -> ");
      sb.append(edge.getSuccessor().getNodeNumber());
      sb.append(" [label=\"");

      // the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
      // future updates of dotty may make this obsolete.
      sb.append(escapeGraphvizLabel(edge.getDescription(), " "));
      sb.append("\"");

      if (edge instanceof FunctionSummaryEdge) {
        sb.append(" style=\"dotted\" arrowhead=\"empty\"");
      }
      sb.append("]");
      return sb.toString();
    }
  }

  static String formatNode(CFANode node, Optional<ImmutableSet<CFANode>> loopHeads) {
    return formatNode(node, loopHeads, DOTBuilder::formatNode);
  }

  static String formatNode(
      CFANode node,
      Optional<ImmutableSet<CFANode>> loopHeads,
      Function<CFANode, String> formatNodeLabel) {
    final String shape;

    String nodeAnnotation = nullToEmpty(formatNodeLabel.apply(node));

    if (nodeAnnotation.length() > NODE_SHAPE_CHANGE_CHAR_LIMIT) {
      shape = "box";
    } else {
      if (loopHeads.isPresent() && loopHeads.orElseThrow().contains(node)) {
        shape = "doublecircle";
      } else if (node.isLoopStart()) {
        shape = "doubleoctagon";

      } else if (node.getNumLeavingEdges() > 0
          && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge) {
        shape = "diamond";
      } else {
        shape = "circle";
      }
    }

    return node.getNodeNumber()
        + " [shape=\""
        + shape
        + "\" label=\""
        + escapeGraphvizLabel(nodeAnnotation, "\\\\n")
        + "\"]";
  }

  public static String escapeGraphvizLabel(String input, String newlineReplacement) {
    // The first call to replace replaces \" with \ " to prevent a bug in dotty.
    // Future updates of dotty may make this obsolete.
    // The next call escapes " with \".
    return input.replace("\\\"", "\\ \"").replace("\"", "\\\"").replace("\n", newlineReplacement);
  }
}
