/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.w3c.dom.Element;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class ARGPathExport {

  private void appendNewEdge(GraphMlBuilder doc, String from, String to, CFAEdge edge) throws IOException {
    Element result = doc.createEdgeElement(from, to);

    if (edge instanceof AssumeEdge) {
      AssumeEdge a = (AssumeEdge) edge;
      result.setAttribute("negation", a.getTruthAssumption() ? "false" : "true");
    }

    doc.addDataElementChild(result, KeyDef.SOURCECODE, edge.getCode());

    Set<Integer> tokens = CFAUtils.getTokensFromCFAEdge(edge);
    doc.addDataElementChild(result, KeyDef.TOKENS, tokensToText(tokens));

    doc.appendToAppendable(result);
  }

  private void appendKeyDefinitions(GraphMlBuilder doc, GraphType graphType) {
    if (graphType == GraphType.CONDITION) {
      doc.appendNewKeyDef(KeyDef.ASSUMPTION, null);
      doc.appendNewKeyDef(KeyDef.INVARIANT, null);
      doc.appendNewKeyDef(KeyDef.NAMED, null);
    }
    doc.appendNewKeyDef(KeyDef.ENTRYNODE, null);
    doc.appendNewKeyDef(KeyDef.SOURCECODE, null);
    doc.appendNewKeyDef(KeyDef.TOKENS, null);
    doc.appendNewKeyDef(KeyDef.NODETYPE, NodeType.ONPATH.text);
  }

  private String tokensToText(Set<Integer> tokens) {
    StringBuilder result = new StringBuilder();
    RangeSet<Integer> tokenRanges = TreeRangeSet.create();
    for (Integer token: tokens) {
      tokenRanges.add(Range.closed(token, token));
    }
    for (Range<Integer> range : tokenRanges.asRanges()) {
      if (result.length() > 0) {
        result.append(",");
      }
      Integer from = range.lowerEndpoint();
      Integer to = range.upperEndpoint();
      if (to - from == 0) {
        result.append(from);
      } else {
        result.append(from);
        result.append("-");
        result.append(to);
      }
    }

    return result.toString();
  }

  private String getStateIdent(ARGState state) {
    return getStateIdent(state, "");
  }

  private String getStateIdent(ARGState state, String identPostfix) {
    return String.format("A%d%s", state.getStateId(), identPostfix);
  }

  private String getPseudoStateIdent(ARGState state, int subStateNo, int subStateCount)
  {
    return getStateIdent(state, String.format("_%d_%d", subStateNo, subStateCount));
  }

  public void writePath(Appendable sb,
      final ARGState rootState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      final Predicate<? super ARGState> displayedElements,
      final Predicate<? super Pair<ARGState, ARGState>> pathEdges)
      throws IOException {

    Set<ARGState> processed = new HashSet<>();
    Deque<ARGState> worklist = new ArrayDeque<>();
    worklist.add(rootState);

    GraphType graphType = GraphType.PROGRAMPATH;

    GraphMlBuilder doc;
    try {
      doc = new GraphMlBuilder(sb);
    } catch (ParserConfigurationException e) {
      throw new IOException(e);
    }

    // TODO: Full schema details
    // Version of format..
    // TODO! (we could use the version of a XML schema)

    // ...
    String entryStateNodeId = getStateIdent(rootState);
    boolean sinkNodeWritten = false;
    int multiEdgeCount = 0; // see below

    doc.appendDocHeader();
    appendKeyDefinitions(doc, graphType);
    doc.appendGraphHeader(graphType, entryStateNodeId, "C");

    while (!worklist.isEmpty()) {
      ARGState s = worklist.removeLast();

      if (!displayedElements.apply(s)) {
        continue;
      }
      if (!processed.add(s)) {
        continue;
      }

      // Location of the state
      CFANode loc = AbstractStates.extractLocation(s);

      // Write the state
      String sourceStateNodeId = getStateIdent(s);
      doc.appendNewNode(sourceStateNodeId, NodeType.ONPATH);

      for (ARGState child : successorFunction.apply(s)) {

        // The child might be covered by another state
        // --> switch to the covering state
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        String childStateId = getStateIdent(child);
        CFANode childLoc = AbstractStates.extractLocation(child);
        CFAEdge edgeToNextState = loc.getEdgeTo(childLoc);
        String prevStateId = sourceStateNodeId;

        if (edgeToNextState instanceof MultiEdge) {
          // The successor state might have several incoming MultiEdges.
          // In this case the state names like ARG<successor>_0 would occur
          // several times.
          // So we add this counter to the state names to make them unique.
          multiEdgeCount++;

          // Write out a long linear chain of pseudo-states (one state encodes multiple edges)
          // because the AutomatonCPA also iterates through the MultiEdge.
          List<CFAEdge> edges = ((MultiEdge)edgeToNextState).getEdges();

          // inner part (without last edge)
          for (int i = 0; i < edges.size()-1; i++) {
            CFAEdge innerEdge = edges.get(i);
            String pseudoStateId = getPseudoStateIdent(child, i, multiEdgeCount);

            doc.appendNewNode(pseudoStateId, NodeType.ONPATH);
            appendNewEdge(doc, prevStateId, pseudoStateId, innerEdge);
            prevStateId = pseudoStateId;
          }

          // last edge connecting it with the real successor
          edgeToNextState = edges.get(edges.size()-1);
        }

        // Only proceed with this state if the path states contains the child
        boolean isEdgeOnPath = pathEdges.apply(Pair.of(s, child));
        if (s.getChildren().contains(child)) {
          if (isEdgeOnPath) {
            // Child belongs to the path!
            appendNewEdge(doc, prevStateId, childStateId, edgeToNextState);
          } else {
            // Child does not belong to the path --> add a branch to the SINK node!
            if (!sinkNodeWritten) {
              sinkNodeWritten = true;
              doc.appendNewNode(SINK_NODE_ID, NodeType.SINKNODE);
            }
          }
        }

        worklist.add(child);
      }
    }

    doc.appendFooter();
  }


}
