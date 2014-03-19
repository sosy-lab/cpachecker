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
package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.JSON;
import org.sosy_lab.common.io.Path;
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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Generates one DOT file per function for the report.
 * For large programs the traditional method with one single DOT file
 * crashed graphviz with "error in init rank" (quite popular in the graphviz bugtracker).
 *
 * Additionally, information about the CFA is exported in JSON format (also needed for the report).
 *
 * Linear sequences of "normal" edges(StatementEdges, DeclarationEdges, and BlankEdges)
 * are displayed as a node containing a table. The left column contains the node number
 * of the predecessor of an edge. The right column contains the edge label.
 * The successor can be found in the left column of the next row.
 *
 * reuses some code from DOTBuilder
 */
public final class DOTBuilder2 {

  private DOTBuilder2() { /* utility class */ }

  /**
   * output the CFA as DOT and JSON files
   *
   * @param cfa
   * @param outdir
   * @throws IOException
   */
  public static void writeReport(CFA cfa, Path outdir) throws IOException {
    CFAJSONBuilder jsoner = new CFAJSONBuilder();
    DOTViewBuilder dotter = new DOTViewBuilder(cfa);
    CFAVisitor vis = new NodeCollectingCFAVisitor(new CompositeCFAVisitor(jsoner, dotter));
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverse(entryNode, vis);
      dotter.writeFunctionFile(entryNode.getFunctionName(), outdir);
    }
    dotter.writeGlobalFiles(outdir);
    JSON.writeJSONString(jsoner.getJSON(), outdir.resolve("cfainfo.json"));
  }

  private static String getEdgeText(CFAEdge edge) {
    //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
    //future updates of dotty may make this obsolete.
    return edge.getDescription()
      .replaceAll("\\Q\\\"\\E", "\\ \"")
      .replaceAll("\\\"", "\\\\\\\"")
      .replaceAll("\n", " ")
      .replaceAll("\\s+", " ")
      .replaceAll(" ;", ";");
  }

  /**
   * output DOT files and meta information about virtual and combined edges
   */
  private static class DOTViewBuilder extends DefaultCFAVisitor {
    // global state for all functions
    private final Map<Object, Object> node2combo = new HashMap<>();
    private final Map<Object, Object> virtFuncCallEdges = new HashMap<>();
    private int virtFuncCallNodeIdCounter = 100000;

    // local state per function
    private final Set<CFANode> nodes = Sets.newLinkedHashSet();
    private final List<CFAEdge> edges = Lists.newArrayList();
    private final List<List<CFAEdge>> comboedges = Lists.newArrayList();

    private List<CFAEdge> currentComboEdge = null;

    private final Optional<ImmutableSet<CFANode>> loopHeads;

    private DOTViewBuilder(CFA cfa) {
      loopHeads = cfa.getAllLoopHeads();
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      CFANode predecessor = edge.getPredecessor();

      // check if it qualifies for a comboEdge
      if (    predecessor.isLoopStart()
          || (predecessor.getNumEnteringEdges() != 1)
          || (predecessor.getNumLeavingEdges() != 1)
          || (currentComboEdge != null && !predecessor.equals(currentComboEdge.get(currentComboEdge.size()-1).getSuccessor()))
          || (edge.getEdgeType() == CFAEdgeType.CallToReturnEdge)
          || (edge.getEdgeType() == CFAEdgeType.AssumeEdge)) {
        // no, it does not

        edges.add(edge);
        currentComboEdge = null;

        // nodes are only added if they are not hidden by a comboedge
        nodes.add(predecessor);
        nodes.add(edge.getSuccessor());

      } else {
        // add combo edge
        if (currentComboEdge == null) {
          currentComboEdge = Lists.newArrayList();
          comboedges.add(currentComboEdge);
        }
        currentComboEdge.add(edge);
      }

      return TraversalProcess.CONTINUE;
    }

    void writeFunctionFile(String funcname, Path outdir) throws IOException {

        try (Writer out = outdir.resolve("cfa__" + funcname + ".dot").asCharSink(Charsets.UTF_8).openStream()) {
          out.write("digraph " + funcname + " {\n");
          StringBuilder outb = new StringBuilder();
          //write comboedges
          for (List<CFAEdge> combo: comboedges) {
            if (combo.size() == 1) {
              edges.add(combo.get(0));
              nodes.add(combo.get(0).getPredecessor());
              nodes.add(combo.get(0).getSuccessor());

            } else {
              outb.append(comboToDot(combo));

              CFAEdge first = combo.get(0);
              CFAEdge last = combo.get(combo.size() - 1);

              outb.append(first.getPredecessor().getNodeNumber());
              outb.append(" -> ");
              outb.append(last.getSuccessor().getNodeNumber());
              outb.append("[label=\"\"]\n");
            }
          }

          //write nodes
          for (CFANode node: nodes) {
            out.write(DOTBuilder.formatNode(node, loopHeads));
          }

          out.write(outb.toString());

          //write edges
          for (CFAEdge edge: edges) {
            out.write(edgeToDot(edge));
          }
          out.write("}");

          nodes.clear();
          edges.clear();
          comboedges.clear();
        }
    }

    void writeGlobalFiles(Path outdir) throws IOException {
      JSON.writeJSONString(node2combo, outdir.resolve("combinednodes.json"));
      JSON.writeJSONString(virtFuncCallEdges, outdir.resolve("fcalledges.json"));
    }

    private String edgeToDot(CFAEdge edge) {
      if (edge.getEdgeType() == CFAEdgeType.CallToReturnEdge) {
       //create the function node
        String calledFunction = edge.getPredecessor().getLeavingEdge(0).getSuccessor().getFunctionName();
        String ret = (++virtFuncCallNodeIdCounter) + " [shape=\"component\" label=\"" + calledFunction + "\"]\n";
        int from = edge.getPredecessor().getNodeNumber();
        ret += String.format("%d -> %d [label=\"%s\" fontname=\"Courier New\"]%n",
            from,
            virtFuncCallNodeIdCounter,
            getEdgeText(edge));

        int to = edge.getSuccessor().getNodeNumber();
        ret += String.format("%d -> %d [label=\"\" fontname=\"Courier New\"]%n",
            virtFuncCallNodeIdCounter,
            to);
        virtFuncCallEdges.put(from, Lists.newArrayList(virtFuncCallNodeIdCounter, to));
        return ret;
      }

      return String.format("%d -> %d [label=\"%s\" fontname=\"Courier New\"]%n",
          edge.getPredecessor().getNodeNumber(),
          edge.getSuccessor().getNodeNumber(),
          getEdgeText(edge));
    }

    private String comboToDot(List<CFAEdge> combo) {
      CFAEdge first = combo.get(0);
      StringBuilder sb = new StringBuilder();
      int firstNo = first.getPredecessor().getNodeNumber();
      sb.append(firstNo);
      sb.append(" [style=\"filled,bold\" penwidth=\"1\" fillcolor=\"white\" fontname=\"Courier New\" shape=\"Mrecord\" label=");

      if (combo.size() > 20) {
        // edge too long, dotty won't be able to handle it
        // 20 is just a guess
        CFAEdge last = combo.get(combo.size()-1);
        int lastNo = last.getPredecessor().getNodeNumber();

        sb.append("\"Long linear chain of edges between nodes ");
        sb.append(firstNo);
        sb.append(" and ");
        sb.append(lastNo);
        sb.append('"');

      } else {
        sb.append("<<table border=\"0\" cellborder=\"0\" cellpadding=\"3\" bgcolor=\"white\">");

        for (CFAEdge edge: combo) {
          sb.append("<tr><td align=\"right\">");
          sb.append("" + edge.getPredecessor().getNodeNumber());
          sb.append("</td><td align=\"left\">");
          sb.append("" + getEdgeText(edge)
                            .replaceAll("\\|", "&#124;")
                            .replaceAll("&", "&amp;")
                            .replaceAll("<", "&lt;")
                            .replaceAll(">", "&gt;")
                            .replaceAll("\\{", "&#123;")
                            .replaceAll("\\}", "&#125;"));
          sb.append("</td></tr>");
        }
        sb.append("</table>>");
      }

      for (CFAEdge edge: combo) {
        node2combo.put(edge.getPredecessor().getNodeNumber(), firstNo);
      }

      sb.append("]\n");
      return sb.toString();
    }

  }

  /**
   * output information about CFA nodes and edges as JSON
   */
  private static class CFAJSONBuilder extends DefaultCFAVisitor {
    private final Map<Object, Object> nodes = new HashMap<>();
    private final Map<Object, Object> edges = new HashMap<>();

    @Override
    public TraversalProcess visitNode(CFANode node) {
      Map<String, Object> jnode = new HashMap<>();
      jnode.put("no", node.getNodeNumber());
      jnode.put("func", node.getFunctionName());
      nodes.put(node.getNodeNumber(), jnode);

      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      Map<String, Object> jedge = new HashMap<>();
      int src = edge.getPredecessor().getNodeNumber();
      int target = edge.getSuccessor().getNodeNumber();
      jedge.put("line", edge.getLineNumber());
      jedge.put("source", src);
      jedge.put("target", target);
      jedge.put("stmt", getEdgeText(edge));
      jedge.put("type", edge.getEdgeType().toString());

      edges.put("" + src + "->" + target, jedge);

      return TraversalProcess.CONTINUE;
    }

    Map<String, Object> getJSON() {
      Map<String, Object> obj = new HashMap<>();
      obj.put("nodes", nodes);
      obj.put("edges", edges);
      return obj;
    }
  }
}
