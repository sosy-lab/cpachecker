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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.json.simple.JSONObject;
import org.sosy_lab.common.Files;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

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
  public static void writeReport(CFA cfa, File outdir) throws IOException {
    CFAJSONBuilder jsoner = new CFAJSONBuilder();
    DOTViewBuilder dotter = new DOTViewBuilder();
    CFAVisitor vis = new NodeCollectingCFAVisitor(new CompositeCFAVisitor(jsoner, dotter));
    for (CFAFunctionDefinitionNode entryNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverse(entryNode, vis);
    }
    dotter.writeFiles(outdir);
    Files.writeFile(new File(outdir, "cfainfo.json"), jsoner.getJSON().toJSONString());
  }

  private static String getEdgeText(CFAEdge edge) {
    //the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
    //future updates of dotty may make this obsolete.
    return edge.getDescription()
      .replaceAll("\\Q\\\"\\E", "\\ \"")
      .replaceAll ("\\\"", "\\\\\\\"")
      .replaceAll("\n", " ")
      .replaceAll("\\s+", " ")
      .replaceAll(" ;", ";");
  }

  /**
   * output DOT files and meta information about virtual and combined edges
   */
  private static class DOTViewBuilder extends DefaultCFAVisitor {

    private final ListMultimap<String, CFANode> func2nodes = ArrayListMultimap.create();
    private final ListMultimap<String, CFAEdge> func2edges = ArrayListMultimap.create();
    private final ListMultimap<String, List<CFAEdge>> func2comboedge = ArrayListMultimap.create();
    private final JSONObject node2combo = new JSONObject();
    private final JSONObject virtFuncCallEdges = new JSONObject();
    private int virtFuncCallNodeIdCounter = 100000;

    private List<CFAEdge> currentComboEdge = null;

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      CFANode predecessor = edge.getPredecessor();

      // check if it qualifies for a comboEdge
      if (    predecessor.isLoopStart()
          || (predecessor.getNumEnteringEdges() != 1)
          || (predecessor.getNumLeavingEdges() != 1)
          || (currentComboEdge != null && !predecessor.equals(currentComboEdge.get(currentComboEdge.size()-1).getSuccessor()))
          || (edge instanceof CallToReturnEdge)
          || (edge instanceof AssumeEdge)) {
        // no, it does not

        func2nodes.put(predecessor.getFunctionName(), predecessor);
        func2edges.put(predecessor.getFunctionName(), edge);
        currentComboEdge = null;

      } else {
        // add combo edge
        if (currentComboEdge == null) {
          currentComboEdge = Lists.newArrayList();
          func2comboedge.put(predecessor.getFunctionName(), currentComboEdge);
        }
        currentComboEdge.add(edge);
      }

      // last, handle successor if necessary
      CFANode successor = edge.getSuccessor();
      if (successor.getNumLeavingEdges() == 0 && successor.getLeavingSummaryEdge() == null) {
        func2nodes.put(successor.getFunctionName(), successor);
        currentComboEdge = null;
      }

      return TraversalProcess.CONTINUE;
    }

    void writeFiles(File outdir) throws IOException {

      for (String funcname: func2nodes.keySet()) {
        List<CFANode> nodes = func2nodes.get(funcname);
        List<CFAEdge> edges = func2edges.get(funcname);

        Writer out = new OutputStreamWriter(new FileOutputStream(new File(outdir, "cfa__" + funcname + ".dot")), "UTF-8");
        try {
          out.write("digraph " + funcname + " {\n");
          StringBuilder outb = new StringBuilder();
          //write comboedges
          for (List<CFAEdge> combo: func2comboedge.get(funcname)) {
            if (combo.size() == 1) {
              edges.add(combo.get(0));
              nodes.add(combo.get(0).getPredecessor());
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
            out.write(nodeToDot(node));
          }

          out.write(outb.toString());

          //write edges
          for (CFAEdge edge: edges) {
            out.write(edgeToDot(edge));
          }
          out.write("}");
        } finally {
          out.close();
        }
      }

      Files.writeFile(new File(outdir, "combinednodes.json"), node2combo.toJSONString());
      Files.writeFile(new File(outdir, "fcalledges.json"),    virtFuncCallEdges.toJSONString());
    }

    private static String nodeToDot(CFANode node) {
      String shape = "circle";

      if(node.isLoopStart()){
        shape = "doublecircle";
      } else if (node.getNumLeavingEdges() > 0 &&
          node.getLeavingEdge(0) instanceof AssumeEdge) {
        shape = "diamond";
      }

      return node.getNodeNumber() + " [shape=\"" + shape + "\"]\n";
    }

    @SuppressWarnings("unchecked")
    private String edgeToDot(CFAEdge edge) {
      if (edge instanceof CallToReturnEdge) {
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

    @SuppressWarnings("unchecked")
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
    private final JSONObject nodes = new JSONObject();
    private final JSONObject edges = new JSONObject();

    @Override
    @SuppressWarnings("unchecked")
    public TraversalProcess visitNode(CFANode node) {
      JSONObject jnode = new JSONObject();
      jnode.put("no", node.getNodeNumber());
      jnode.put("line", node.getLineNumber());
      jnode.put("func", node.getFunctionName());
      nodes.put(node.getNodeNumber(), jnode);

      return TraversalProcess.CONTINUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TraversalProcess visitEdge(CFAEdge edge) {
      JSONObject jedge = new JSONObject();
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

    @SuppressWarnings("unchecked")
    JSONObject getJSON() {
      JSONObject obj = new JSONObject();
      obj.put("nodes", nodes);
      obj.put("edges", edges);
      return obj;
    }
  }
}
