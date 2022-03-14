// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.CFAUtils.successorsOf;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

/**
 * Generates one DOT file per function for the report. For large programs the traditional method
 * with one single DOT file crashed graphviz with "error in init rank" (quite popular in the
 * graphviz bugtracker).
 *
 * <p>Additionally, information about the CFA is exported in JSON format (also needed for the
 * report).
 *
 * <p>Linear sequences of "normal" edges(StatementEdges, DeclarationEdges, and BlankEdges) are
 * displayed as a node containing a table. The left column contains the node number of the
 * predecessor of an edge. The right column contains the edge label. The successor can be found in
 * the left column of the next row.
 *
 * <p>reuses some code from DOTBuilder
 */
public final class DOTBuilder2 {

  private final CFA cfa;
  private final CFAJSONBuilder jsoner;
  private final DOTViewBuilder dotter;

  public DOTBuilder2(CFA pCfa) {
    cfa = checkNotNull(pCfa);
    jsoner = new CFAJSONBuilder();
    dotter = new DOTViewBuilder(cfa);
    CFAVisitor vis = new NodeCollectingCFAVisitor(new CompositeCFAVisitor(jsoner, dotter));
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverse(entryNode, vis);
    }
    dotter.postProcessing();
  }

  /** output the CFA as DOT files */
  public void writeGraphs(Path outdir) throws IOException {
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      dotter.writeFunctionFile(entryNode.getFunctionName(), outdir);
    }
  }

  public void writeCfaInfo(Writer out) throws IOException {
    out.write("\"nodes\":");
    JSON.writeJSONString(jsoner.getNodes(), out);
    out.write(",\n\"edges\":");
    JSON.writeJSONString(jsoner.getEdges(), out);
  }

  public void writeFunctionCallEdges(Writer out) throws IOException {
    JSON.writeJSONString(dotter.virtFuncCallEdges, out);
  }

  public void writeCombinedNodes(Writer out) throws IOException {
    JSON.writeJSONString(dotter.comboNodes, out);
  }

  public void writeCombinedNodesLabels(Writer out) throws IOException {
    JSON.writeJSONString(dotter.comboNodesLabels, out);
  }

  public void writeMergedNodesList(Writer out) throws IOException {
    JSON.writeJSONString(dotter.mergedNodes, out);
  }

  private static String getEdgeText(CFAEdge edge) {
    return DOTBuilder.escapeGraphvizLabel(edge.getDescription(), " ");
  }

  /** output DOT files and meta information about virtual and combined edges */
  private static class DOTViewBuilder extends DefaultCFAVisitor {
    // global state for all functions
    private final Map<Integer, Set<Integer>> comboNodes = new HashMap<>();
    private final Map<Integer, StringBuilder> comboNodesLabels = new HashMap<>();
    private final Set<Integer> mergedNodes = new LinkedHashSet<>();
    private final Map<Integer, List<Integer>> virtFuncCallEdges = new HashMap<>();
    private int virtFuncCallNodeIdCounter = 100000;

    // local state per function
    private final SetMultimap<String, CFANode> nodes =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();
    private final ListMultimap<String, CFAEdge> edges =
        MultimapBuilder.hashKeys().arrayListValues().build();
    private final ListMultimap<String, List<CFAEdge>> comboedges =
        MultimapBuilder.hashKeys().arrayListValues().build();

    private List<CFAEdge> currentComboEdge = null;

    private final Optional<ImmutableSet<CFANode>> loopHeads;

    private DOTViewBuilder(CFA cfa) {
      loopHeads = cfa.getAllLoopHeads();
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      CFANode predecessor = edge.getPredecessor();
      String funcname = predecessor.getFunctionName();

      // check if it qualifies for a comboEdge
      if (predecessor.isLoopStart()
          || (predecessor.getNumEnteringEdges() != 1)
          || (predecessor.getNumLeavingEdges() != 1)
          || (currentComboEdge != null
              && !predecessor.equals(
                  currentComboEdge.get(currentComboEdge.size() - 1).getSuccessor()))
          || (edge.getEdgeType() == CFAEdgeType.CallToReturnEdge)
          || (edge.getEdgeType() == CFAEdgeType.AssumeEdge)) {
        // no, it does not

        edges.put(funcname, edge);
        currentComboEdge = null;

        // nodes are only added if they are not hidden by a comboedge
        nodes.put(funcname, predecessor);
        nodes.put(funcname, edge.getSuccessor());

      } else {
        // add combo edge
        if (currentComboEdge == null) {
          currentComboEdge = new ArrayList<>();
          comboedges.put(funcname, currentComboEdge);
        }
        currentComboEdge.add(edge);
      }

      return TraversalProcess.CONTINUE;
    }

    @SuppressWarnings("null")
    private void postProcessing() {
      Iterator<Entry<String, List<CFAEdge>>> it = comboedges.entries().iterator();
      while (it.hasNext()) {
        Entry<String, List<CFAEdge>> entry = it.next();
        List<CFAEdge> combinedEdges = entry.getValue();
        String functionName = entry.getKey();

        if (combinedEdges.size() == 1) {
          it.remove();
          CFAEdge firstEdge = combinedEdges.get(0);
          edges.put(functionName, firstEdge);
          nodes.put(functionName, firstEdge.getPredecessor());
          nodes.put(functionName, firstEdge.getSuccessor());

        } else if (combinedEdges.size() > 1) {
          CFAEdge first = combinedEdges.get(0);
          int firstNode = first.getPredecessor().getNodeNumber();
          Set<Integer> combinedNodes = comboNodes.get(firstNode);
          StringBuilder label = comboNodesLabels.get(firstNode);
          // Initialize the list of nodes and the label if necessary
          if (combinedNodes == null) {
            assert label == null
                : "label and combinedNodes should always be initialized and changed together";
            combinedNodes = new LinkedHashSet<>();
            comboNodes.put(firstNode, combinedNodes);
            label = new StringBuilder();
            comboNodesLabels.put(firstNode, label);
          }
          for (CFAEdge edge : combinedEdges) {
            int predNumber = edge.getPredecessor().getNodeNumber();
            // If we have not added this node yet,
            // add it and extend the label
            if (combinedNodes.add(predNumber)) {
              // If this is not the first element we combine,
              // we should continue the description in a new line
              if (combinedNodes.size() > 1) {
                label.append("\n");
              }
              label.append(predNumber);
              label.append(" ");
              label.append(edge.getDescription());
            }
          }
          comboNodes.forEach(
              (k, v) -> {
                v.remove(k);
                mergedNodes.addAll(v);
              });
        }
      }
      for (CFAEdge edge : edges.values()) {
        if (edge.getEdgeType() == CFAEdgeType.CallToReturnEdge) {
          int from = edge.getPredecessor().getNodeNumber();
          int to = edge.getSuccessor().getNodeNumber();
          virtFuncCallEdges.put(from, Lists.newArrayList(++virtFuncCallNodeIdCounter, to));
        }
      }
    }

    void writeFunctionFile(String funcname, Path outdir) throws IOException {

      Path cfaFile = outdir.resolve("cfa__" + funcname + ".dot");
      MoreFiles.createParentDirectories(cfaFile);
      try (Writer out = Files.newBufferedWriter(cfaFile, StandardCharsets.UTF_8)) {
        out.write("digraph " + funcname + " {\n");
        StringBuilder outb = new StringBuilder();

        // write comboedges
        for (List<CFAEdge> combo : comboedges.get(funcname)) {
          outb.append(comboToDot(combo));

          CFAEdge first = combo.get(0);
          CFAEdge last = combo.get(combo.size() - 1);

          outb.append(first.getPredecessor().getNodeNumber());
          outb.append(" -> ");
          outb.append(last.getSuccessor().getNodeNumber());
          outb.append("[label=\"\"]\n");
        }

        // write nodes
        for (CFANode node : nodes.get(funcname)) {
          out.write(DOTBuilder.formatNode(node, loopHeads));
          out.write('\n');
        }

        out.write(outb.toString());

        // write edges
        for (CFAEdge edge : edges.get(funcname)) {
          out.write(edgeToDot(edge));
        }
        out.write("}");
      }
    }

    private String edgeToDot(CFAEdge edge) {
      if (edge.getEdgeType() == CFAEdgeType.CallToReturnEdge) {
        // create the function node
        CFANode functionEntryNode =
            getOnlyElement(successorsOf(edge.getPredecessor()).filter(FunctionEntryNode.class));
        String calledFunction = functionEntryNode.getFunctionName();
        int from = edge.getPredecessor().getNodeNumber();
        Integer virtFuncCallNodeId = virtFuncCallEdges.get(from).get(0);

        String ret =
            virtFuncCallNodeId + " [shape=\"component\" label=\"" + calledFunction + "\"]\n";
        ret +=
            String.format(
                "%d -> %d [label=\"%s\" fontname=\"Courier New\"]%n",
                from, virtFuncCallNodeId, getEdgeText(edge));

        int to = edge.getSuccessor().getNodeNumber();
        ret +=
            String.format(
                "%d -> %d [label=\"\" fontname=\"Courier New\"]%n", virtFuncCallNodeId, to);
        return ret;
      }

      return String.format(
          "%d -> %d [label=\"%s\" fontname=\"Courier New\"]%n",
          edge.getPredecessor().getNodeNumber(),
          edge.getSuccessor().getNodeNumber(),
          getEdgeText(edge));
    }

    private String comboToDot(List<CFAEdge> combo) {
      CFAEdge first = combo.get(0);
      StringBuilder sb = new StringBuilder();
      int firstNo = first.getPredecessor().getNodeNumber();
      sb.append(firstNo);
      sb.append(
          " [style=\"filled,bold\" penwidth=\"1\" fillcolor=\"white\" fontname=\"Courier New\""
              + " shape=\"Mrecord\" label=");

      if (combo.size() > 20) {
        // edge too long, dotty won't be able to handle it
        // 20 is just a guess
        CFAEdge last = combo.get(combo.size() - 1);
        int lastNo = last.getPredecessor().getNodeNumber();

        sb.append("\"Long linear chain of edges between nodes ");
        sb.append(firstNo);
        sb.append(" and ");
        sb.append(lastNo);
        sb.append('"');

      } else {
        sb.append("<<table border=\"0\" cellborder=\"0\" cellpadding=\"3\" bgcolor=\"white\">");

        for (CFAEdge edge : combo) {
          sb.append("<tr><td align=\"right\">");
          sb.append("" + edge.getPredecessor().getNodeNumber());
          sb.append("</td><td align=\"left\">");
          sb.append(
              HtmlEscapers.htmlEscaper()
                  .escape(getEdgeText(edge))
                  .replace("|", "&#124;")
                  .replace("{", "&#123;")
                  .replace("}", "&#125;"));
          sb.append("</td></tr>");
        }
        sb.append("</table>>");
      }

      sb.append("]\n");
      return sb.toString();
    }
  }

  /** output information about CFA nodes and edges as JSON */
  private static class CFAJSONBuilder extends DefaultCFAVisitor {
    private final Map<Integer, Object> nodes = new HashMap<>();
    private final Map<String, Object> edges = new HashMap<>();

    @Override
    public TraversalProcess visitNode(CFANode node) {
      Map<String, Object> jnode = new HashMap<>();
      jnode.put("index", node.getNodeNumber());
      jnode.put("rpid", node.getReversePostorderId());
      jnode.put("func", node.getFunctionName());
      jnode.put("type", determineNodeType(node));
      jnode.put("loop", node.isLoopStart());

      nodes.put(node.getNodeNumber(), jnode);

      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      Map<String, Object> jedge = new HashMap<>();
      int src = edge.getPredecessor().getNodeNumber();
      int target = edge.getSuccessor().getNodeNumber();
      jedge.put("line", edge.getFileLocation().getStartingLineInOrigin());
      jedge.put("file", edge.getFileLocation().getFileName());
      jedge.put("source", src);
      jedge.put("target", target);
      jedge.put("stmt", getEdgeText(edge));
      jedge.put("type", edge.getEdgeType().toString());

      edges.put("" + src + "->" + target, jedge);

      return TraversalProcess.CONTINUE;
    }

    private String determineNodeType(CFANode node) {
      if (node instanceof FunctionEntryNode) {
        return "entry";
      } else if (node instanceof FunctionExitNode) {
        return "exit";
      }
      return "";
    }

    Collection<Object> getNodes() {
      return nodes.values();
    }

    Collection<Object> getEdges() {
      return edges.values();
    }
  }
}
