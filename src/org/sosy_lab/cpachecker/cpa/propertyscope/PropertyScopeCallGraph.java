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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PropertyScopeCallGraph {

  private final Map<String, FunctionNode> nodes = new TreeMap<>();
  private final Set<CallEdge> edges = new HashSet<>();
  private FunctionNode entryNode;

  public static PropertyScopeCallGraph create(ARGState root) {
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(root);
    PropertyScopeCallGraph graph = new PropertyScopeCallGraph();

    // handle entry name
    CallstackState rootCsState = extractStateByType(root, CallstackState.class);
    graph.entryNode = graph.functionNodeFor(rootCsState.getCurrentFunction());
    graph.entryNode.calledCount = 1;

    while (!waitlist.isEmpty()) {
      ARGState argState = waitlist.removeFirst();
      CallstackState csState = extractStateByType(argState, CallstackState.class);

      for (ARGState childState: argState.getChildren()) {

        // handle name calls
        CallstackState chCsState = extractStateByType(childState, CallstackState.class);
        if (chCsState.getDepth() > csState.getDepth()) {
          CallEdge callEdge =
              graph.callEdgeFor(csState.getCurrentFunction(), chCsState.getCurrentFunction());
          callEdge.sink.calledCount += 1;
        }

        PropertyScopeState psState = extractStateByType(childState, PropertyScopeState.class);

        for(CFAEdge toChEdge: argState.getEdgesToChild(childState)) {
          FunctionNode fnode = graph.functionNodeFor(toChEdge.getSuccessor().getFunctionName());
          fnode.cfaEdges += 1;
          fnode.propertyRelevantCFAEdges += psState.getScopeLocations().stream()
              .anyMatch(sloc -> sloc.getEdge().equals(toChEdge)) ? 1 : 0;
        }

        waitlist.addFirst(childState);
      }
    }

    return graph;
  }

  private FunctionNode functionNodeFor(String name) {
    return nodes.computeIfAbsent(name, function -> {
      FunctionNode node = new FunctionNode(function);
      nodes.put(function, node);
      return node;
    });
  }

  private CallEdge callEdgeFor(String source, String sink) {
    return callEdgeFor(functionNodeFor(source), functionNodeFor(sink));
  }

  private CallEdge callEdgeFor(FunctionNode source, FunctionNode sink) {
    return source.callEdges.stream().filter(edge -> edge.sink.equals(sink)).findFirst()
        .orElseGet(() -> {
          CallEdge edge = new CallEdge(source, sink);
          source.callEdges.add(edge);
          edges.add(edge);
          return edge;
        });
  }

  private static class CallEdge {
    private final FunctionNode source;
    private final FunctionNode sink;

    private CallEdge(FunctionNode pSource, FunctionNode pSink) {
      source = pSource;
      sink = pSink;
    }

    public FunctionNode getSource() {
      return source;
    }

    public FunctionNode getSink() {
      return sink;
    }

    @Override
    public String toString() {
      return String.format("Call(%s -> %s)", source.name, sink.name);
    }
  }

  private static class FunctionNode {
    private final String name;
    private int calledCount = 0; // how often the name gets called
    private int cfaEdges = 0; // passed CFAEdges in ARG
    private int propertyRelevantCFAEdges = 0; // cfaEdges which are in scope of the property
    private final Set<CallEdge> callEdges = new HashSet<>();

    private FunctionNode(String pName) {
      name = pName;
    }

    public String getName() {
      return name;
    }

    public int getCalledCount() {
      return calledCount;
    }

    public int getCfaEdges() {
      return cfaEdges;
    }

    public int getPropertyRelevantCFAEdges() {
      return propertyRelevantCFAEdges;
    }

    public Set<CallEdge> getCallEdges() {
      return Collections.unmodifiableSet(callEdges);
    }

    @Override
    public String toString() {
      return String.format("FunctionNode(%s)", name);
    }
  }

  public Map<String, FunctionNode> getNodes() {
    return Collections.unmodifiableMap(nodes);
  }

  public Set<CallEdge> getEdges() {
    return Collections.unmodifiableSet(edges);
  }

  public FunctionNode getEntryNode() {
    return entryNode;
  }

  @Override
  public String toString() {
    return String.format("PropertyScopeCallGraph(nodes=%s, edges=%s)", nodes.size(), edges.size());
  }
}
