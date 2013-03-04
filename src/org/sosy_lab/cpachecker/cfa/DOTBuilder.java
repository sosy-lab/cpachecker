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
package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Class for generating a DOT file from a CFA.
 */
public final class DOTBuilder {

  private DOTBuilder() { /* utility class */ }

  private static final String MAIN_GRAPH = "____Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');

  public static void generateDOT(Appendable sb,
      Collection<FunctionEntryNode> cfasMapList, FunctionEntryNode cfa) throws IOException {
    DotGenerator dotGenerator = new DotGenerator();
    CFATraversal.dfs().traverseOnce(cfa, dotGenerator);

    sb.append("digraph " + "CFA" + " {\n");

    JOINER_ON_NEWLINE.appendTo(sb, dotGenerator.nodes);
    sb.append('\n');

    // define the graphic representation for all subsequent nodes
    sb.append("node [shape=\"circle\"]\n");

    for (FunctionEntryNode fnode : cfasMapList) {
      // If Array belongs to functionCall in Parameter, replace [].
      // If Name Contains '.' replace with '_'
      sb.append("subgraph cluster_" + fnode.getFunctionName()
          .replace("[", "").replace("]", "_array")
          .replace(".", "_") + " {\n");
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

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      CFANode predecessor = edge.getPredecessor();
      if (!predecessor.isLoopStart() && edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        nodes.add(formatNode(predecessor, "diamond"));
      }

      List<String> graph;
      if ((edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) || edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        graph = edges.get(MAIN_GRAPH);
      } else {
        graph = edges.get(predecessor.getFunctionName());
      }
      graph.add(formatEdge(edge));

      return CFATraversal.TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode node) {

      if (node.isLoopStart()) {
        nodes.add(formatNode(node, "doublecircle"));
      }

      return CFATraversal.TraversalProcess.CONTINUE;
    }

    private static String formatNode(CFANode node, String shape) {
      return node.getNodeNumber() + " [shape=\"" + shape + "\"]";
    }

    private static String formatEdge(CFAEdge edge) {
      StringBuilder sb = new StringBuilder();
      sb.append(edge.getPredecessor().getNodeNumber());
      sb.append(" -> ");
      sb.append(edge.getSuccessor().getNodeNumber());
      sb.append(" [label=\"");

      //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
      //future updates of dotty may make this obsolete.
      sb.append(edge.getDescription().replaceAll("\\Q\\\"\\E", "\\ \"")
                                     .replaceAll("\\\"", "\\\\\\\"")
                                     .replaceAll("\n", " "));

      sb.append("\"");
      if (edge instanceof FunctionSummaryEdge) {
        sb.append(" style=\"dotted\" arrowhead=\"empty\"");
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
