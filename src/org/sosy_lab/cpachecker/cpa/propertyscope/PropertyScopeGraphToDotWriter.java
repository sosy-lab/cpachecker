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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.ShadowCFAEdgeFactory.ShadowCFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeGraph.ScopeEdge;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeGraph.ScopeNode;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeInstance.AutomatonPropertyScopeInstance;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class PropertyScopeGraphToDotWriter {

  private final PropertyScopeGraph graph;
  private final boolean hinted;
  private final ImmutableMap<String, String> automatonColorMap;

  public PropertyScopeGraphToDotWriter(PropertyScopeGraph pGraph, boolean pHinted) {
    graph = pGraph;
    hinted = pHinted;

    /* build color for automatons */
    List<String> colorList = Arrays.asList("#aa5500", "#aaaa00", "#aa007f", "#aaaaff", "#ff007f",
        "#ffaaff", "#ffff7f", "#00aaff", "#aaff7f", "#535551");
    Collection<AutomatonState> automatonStates =
        extractStatesByType(pGraph.getRootNode().getArgState(), AutomatonState.class);
    List<String> automNames = automatonStates.stream()
        .map(AutomatonState::getOwningAutomatonName).sorted().collect(Collectors.toList());
    Builder<String, String> colorMBuilder = ImmutableMap.builder();
    Random predictable = new Random(743998);
    for (int i = 0; i < automNames.size(); i++) {
      if (i < colorList.size()) {
        colorMBuilder.put(automNames.get(i), colorList.get(i));
      } else {
        colorMBuilder.put(automNames.get(i), String.format("#%h", predictable.nextInt(0xffffff)));
      }
    }
    automatonColorMap = colorMBuilder.build();

  }

  public static void write(PropertyScopeGraph graph, Appendable sb) throws IOException {
    new PropertyScopeGraphToDotWriter(graph, false).write(sb);
  }

  public static void writeHinted(PropertyScopeGraph graph, Appendable sb) throws IOException {
    new PropertyScopeGraphToDotWriter(graph, true).write(sb);
  }

  public void write(Appendable sb) throws IOException {
    sb // preface
        .append("digraph \"PSARG_")
        .append(graph.getScopeReasons().stream()
            .map(Reason::name).collect(Collectors.joining("_")))
        .append("\" {\n")
        .append("node [style=\"filled\" shape=\"box\" color=\"white\"  ")
        //.append("width=0 height=0 margin=0")
        .append("]\n");

    // build Legend

    sb.append(" { rank = sink;\n").append("   Legend [shape=none, margin=0, label=<");
    sb.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">");
    sb.append("<TR><TD COLSPAN=\"2\"><B>Legend</B></TD></TR>");
    for (Entry<String, String> automCol : automatonColorMap.entrySet()) {
      String automName = automCol.getKey();
      String automColor = automCol.getValue();
      sb.append("<TR><TD>").append(automName).append("</TD>");
      sb.append("<TD ").append("BGCOLOR=\"").append(automColor).append("\"></TD></TR>");
    }


    sb.append("</TABLE>");
    sb.append(">];").append("\n").append(" }\n");


    // specify nodes

    for (ScopeNode scopeNode : graph.getNodes().values()) {
      sb.append("\"").append(scopeNode.getId()).append("\" [");
      bulidNodeParams(scopeNode, sb);
      sb.append("]");

      sb.append(";\n");

      if (hinted) {
        buidNodeHint(scopeNode, sb);
      }
    }

    // specify edges
    for (ScopeEdge scopeEdge : graph.getEdges().values()) {
      sb
          .append("\"").append(scopeEdge.getStart().getId()).append("\" -> \"")
          .append(scopeEdge.getEnd().getId()).append("\"")
          .append(" [");

      buildEdgeParams(scopeEdge, sb);

      sb
          .append("]")
          .append(";\n");
    }

    // end of graph
    sb.append("}\n");


  }

  private void bulidNodeParams(ScopeNode scopeNode, Appendable sb) throws IOException {
    sb.append("label=\"");
    ARGState currentElement = scopeNode.getArgState();
    sb.append(Objects.toString(currentElement.getStateId()));

    Iterable<CFANode> locs = extractLocations(currentElement);
    if (locs != null) {
      for (CFANode loc : extractLocations(currentElement)) {
        sb.append("@");
        sb.append(loc.toString());
        sb.append(" r ");
        sb.append(Objects.toString(loc.getReversePostorderId()));
        if (loc instanceof ShadowCFANode) {
          sb.append(" ~ weaved ");
        }
        sb.append("\\n");
        sb.append(loc.getFunctionName());
        if (loc instanceof FunctionEntryNode) {
          sb.append(" entry");
        } else if (loc instanceof FunctionExitNode) {
          sb.append(" exit");
        }
        sb.append("\\n");
      }
    } else {
      sb.append("\\n");
    }

    for (Reason reason : scopeNode.getScopeReasons()) {
      sb.append(reason.name()).append("\\n");
    }
    Map<Automaton, AutomatonPropertyScopeInstance> automScopeInsts =
        extractStateByType(scopeNode.getArgState(), PropertyScopeState.class).getAutomScopeInsts();

    sb.append("\"");

    if (!scopeNode.isPartOfScope() || automScopeInsts.isEmpty()) {
      sb.append(" color=\"").append(determineNodeColor(scopeNode)).append("\"");
    } else {
      sb.append(" style=\"striped\"");
      String fillcolor = automScopeInsts.keySet().stream()
          .map(autom -> automatonColorMap.get(autom.getName()))
          .collect(Collectors.joining(":"));
      sb.append(" color=\"").append(fillcolor).append("\"");
    }

  }

  private void buidNodeHint(ScopeNode node, Appendable sb) throws IOException {
    final String stateNodeId = node.getId();
    final String hintNodeId = stateNodeId + "hint";

    sb.append("\n {\n  rank=same;\n");
    sb.append("  \"").append(hintNodeId).append("\" [");

    sb.append(" label=").append("\"");

    for (AutomatonState automst :extractStatesByType(node.getArgState(), AutomatonState.class)) {
      sb
          .append(automst.getOwningAutomatonName()).append(": ")
          .append(automst.getInternalStateName()).append("\\n");
    }

    PredicateAbstractState predSt =
        extractStateByType(node.getArgState(), PredicateAbstractState.class);
    sb
        .append("PA: ")
        .append(predSt.getAbstractionFormula().asInstantiatedFormula().toString())
        .append("\\n");

    sb.append("\"").append(" shape=box style=filled fillcolor=\"#d5d5d5\" fontsize=9];\n");

    sb.append("  \"").append(stateNodeId).append("\"");
    sb.append(" -> ");
    sb.append("\"");
    sb.append(hintNodeId);
    sb.append("\"");
    sb.append(" [arrowhead=none color=\"#d5d5d5\" rank=same style=solid];\n }\n");

  }

  private static String determineNodeColor(ScopeNode scopeNode) {

    if (scopeNode.getArgState().isCovered()) {
      return "green";
    }
    if (scopeNode.getArgState().isTarget()) {
      return "red";
    }

    if (scopeNode.isPartOfScope()) {
      return "cornflowerblue";
    }

    return "grey";
  }

  private void buildEdgeParams(ScopeEdge scopeEdge, Appendable sb) throws IOException {
    if (scopeEdge.getIrrelevantARGStates() == 0) {
      List<CFAEdge> edges =
          scopeEdge.getStart().getArgState().getEdgesToChild(scopeEdge.getEnd().getArgState());

      if (edges.isEmpty()) { // there is no direct edge between the nodes, use a dummy-edge
        sb.append("style=\"bold\" color=\"blue\" label=\"dummy edge\"");

      } else { // edge exists, use info from edge

        boolean hasWeavedTrans = false;
        for (CFAEdge edge : edges) {
          if (edge.getPredecessor() instanceof ShadowCFANode) {
            hasWeavedTrans = true;
            break;
          }
        }

        if (hasWeavedTrans) {
          sb.append("color=\"green\" ");
        }

        sb.append("label=\"");
        if (edges.size() > 1) {
          sb
              .append("Lines ")
              .append(Objects.toString(edges.get(0).getLineNumber()))
              .append(" - ")
              .append(Objects.toString(edges.get(edges.size() - 1).getLineNumber()));
        } else {
          sb.append("Line ").append(Objects.toString(edges.get(0).getLineNumber()));
        }
        sb.append(": \\l");

        for (CFAEdge edge : edges) {
          sb.append(edge.getDescription().replaceAll("\n", " ").replace('"', '\''));
          sb.append("\\l");
        }
        sb.append("\"");
      }
    } else {
      sb
          .append("label=\"")
          .append("Irrelevant: ")
          .append(Objects.toString(scopeEdge.getIrrelevantARGStates()))
          .append("\\l");

      if (!scopeEdge.getPassedFunctionEntryExits().isEmpty()) {
        sb
            .append("\\l")
            .append("Skipped entry/exit:\\l");
        for (String func : scopeEdge.getPassedFunctionEntryExits()) {
          sb.append(func).append("\\l");
        }
      }



      if (scopeEdge.getLastCFAEdge().isPresent()) {
        CFAEdge lastCfaEdge = scopeEdge.getLastCFAEdge().get();
        sb
            .append("\\l")
            .append("Line ").append(Objects.toString(lastCfaEdge.getLineNumber()))
            .append(": \\l")
            .append(lastCfaEdge.getDescription().replaceAll("\n", " ").replace('"', '\'').trim())
            .append("\\l");

      }

      sb.append("\"");
      sb.append(" color=\"#55007f\"");
    }

  }
}

