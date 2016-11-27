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
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PropertyScopeCallGraph {

  private final Map<String, FunctionNode> nodes = new TreeMap<>();
  private final SortedMap<Integer, FunctionNode> idNodes = new TreeMap<>();
  private final Set<CallEdge> edges = new HashSet<>();
  private FunctionNode entryNode;

  public static PropertyScopeCallGraph create(ARGState root) {
    return create(root, Collections.emptySet());
  }

  public static PropertyScopeCallGraph create(
      ARGState root, Collection<String> prepopulateFunctions) {
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(root);
    PropertyScopeCallGraph graph = new PropertyScopeCallGraph();
    Set<ARGState> visitedStates = new HashSet<>();

    // prepopulate the graph with the given functions
    prepopulateFunctions.forEach(graph::functionNodeFor);

    // handle entry name
    CallstackState rootCsState = extractStateByType(root, CallstackState.class);
    graph.entryNode = graph.functionNodeFor(rootCsState.getCurrentFunction());
    graph.entryNode.argOccurenceCount = 1;

    while (!waitlist.isEmpty()) {
      ARGState argState = waitlist.removeFirst();
      CallstackState csState = extractStateByType(argState, CallstackState.class);

      if(!visitedStates.contains(argState)) {
        for (ARGState childState : argState.getChildren()) {

          // handle func calls
          CallstackState chCsState = extractStateByType(childState, CallstackState.class);
          if (chCsState.getDepth() > csState.getDepth()) {
            CallEdge callEdge =
                graph.callEdgeFor(csState.getCurrentFunction(), chCsState.getCurrentFunction());
            callEdge.sink.argOccurenceCount += 1;
          }

          PropertyScopeState psState = extractStateByType(childState, PropertyScopeState.class);

          for (CFAEdge toChEdge : argState.getEdgesToChild(childState)) {
            FunctionNode fnode = graph.functionNodeFor(toChEdge.getSuccessor().getFunctionName());
            fnode.cfaEdges += 1;
            for (Reason reason : Reason.values()) {
              if (psState.getScopeLocations().stream()
                  .anyMatch(sloc -> sloc.getEdge().equals(toChEdge)
                      && sloc.getReason() == reason)) {
                fnode.scopedCFAEdges.get(reason).add(toChEdge);
              }
            }
          }

          waitlist.addFirst(childState);
        }

        visitedStates.add(argState);
      }
    }

    return graph;
  }

  private FunctionNode functionNodeFor(String name) {
    return nodes.computeIfAbsent(name, function -> {
      int lastId = idNodes.isEmpty() ? 0 : idNodes.lastKey();
      FunctionNode node = new FunctionNode(lastId + 1, function);
      nodes.put(function, node);
      idNodes.put(node.id, node);
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

  public static class CallEdge {
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

  public static class FunctionNode {
    private final int id;
    private final String name;
    private int argOccurenceCount = 0; // how often the name gets called
    private int cfaEdges = 0; // passed CFAEdges in ARG
    private final Map<ScopeLocation.Reason, Set<CFAEdge>> scopedCFAEdges;

    private final Set<CallEdge> callEdges = new HashSet<>();

    private FunctionNode(int pId, String pName) {
      id = pId;
      name = pName;
      scopedCFAEdges = new TreeMap<>();
      for (Reason r : Reason.values()) {
        scopedCFAEdges.put(r, new LinkedHashSet<>());
      }
    }

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public int getArgOccurenceCount() {
      return argOccurenceCount;
    }

    public int getCfaEdges() {
      return cfaEdges;
    }

    public int getScopedCFAEdgesCount(Collection<Reason> reasons) {
      return getScopedCFAEdges(reasons).size();
    }

    public int getScopedCFAEdgesCount(Reason reason) {
      return getScopedCFAEdgesCount(Collections.singleton(reason));
    }

    public Set<CFAEdge> getScopedCFAEdges(Collection<Reason> reasons) {
      return reasons.stream().flatMap(r -> scopedCFAEdges.get(r).stream())
          .collect(Collectors.toSet());
    }

    public Set<CFAEdge> getScopedCFAEdges(Reason reason) {
      return getScopedCFAEdges(Collections.singleton(reason));
    }

    public double calculatePropertyScopeImportance(Collection<Reason> reasons) {
      return cfaEdges == 0 ? 0 : (double) getScopedCFAEdgesCount(reasons) / (double) cfaEdges;
    }

    public double calculatePropertyScopeImportance(Reason reason) {
      return calculatePropertyScopeImportance(Collections.singleton(reason));
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

  public FunctionNode nodeForId(int id) {
    return idNodes.get(id);
  }

  @Override
  public String toString() {
    return String.format("PropertyScopeCallGraph(nodes=%s, edges=%s)", nodes.size(), edges.size());
  }
}
