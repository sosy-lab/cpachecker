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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeGraph.ScopeEdge.CombiScopeEdge;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertyScopeGraph {

  private final ScopeNode rootNode;
  private final Multimap<ScopeNode, ScopeEdge> edges = LinkedHashMultimap.create();
  private final Map<ARGState, ScopeNode> nodes = new LinkedHashMap<>();
  private final Collection<Reason> scopeReasons;
  private final Table<ScopeNode, ScopeNode, CombiScopeEdge> combiScopeEdges =
      HashBasedTable.create();

  private PropertyScopeGraph(ScopeNode rootNode, Collection<Reason> pScopeReasons) {
    this.rootNode = rootNode;
    scopeReasons = pScopeReasons;
  }

  public static PropertyScopeGraph create(
      ARGState root, Collection<Reason> pScopeReasons, boolean skipUnscopedBranchNodes) {

    PropertyScopeGraph graph = new PropertyScopeGraph(new ScopeNode(root), pScopeReasons);
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Deque<ScopeEdge> currentScopeEdges = new ArrayDeque<>();
    Set<ARGState> visitedStates = new HashSet<>();
    for (ARGState child : root.getChildren()) {
      currentScopeEdges.push(new ScopeEdge(graph.getRootNode()));
      waitlist.addFirst(child);
    }

    while (!waitlist.isEmpty()) {
      ARGState argState = waitlist.removeFirst();

      boolean isVisitedState = visitedStates.contains(argState);
      visitedStates.add(argState);

      CallstackState csState = extractStateByType(argState, CallstackState.class);
      PropertyScopeState psState = extractStateByType(argState, PropertyScopeState.class);
      LocationState locstate = extractStateByType(argState, LocationState.class);

      Set<ScopeLocation> scopeLocations = psState.getScopeLocations().stream()
          .filter(sloc -> pScopeReasons.contains(sloc.getReason()))
          .collect(Collectors.toSet());

      ScopeNode thisScopeNode = graph.nodes.getOrDefault(argState, new ScopeNode(argState));
      scopeLocations.forEach(sloc -> thisScopeNode.scopeReasons.add(sloc.getReason()));

      boolean shouldScopeEdgeEnd = argState.getChildren().isEmpty() || !scopeLocations.isEmpty()
          || (!skipUnscopedBranchNodes && (argState.getChildren().size() > 1 ||
          argState.getParents().size() > 1));

      ScopeEdge currentEdge = currentScopeEdges.peek();
      if (shouldScopeEdgeEnd) {
        currentEdge.end = thisScopeNode;
        graph.edges.put(currentEdge.start, currentEdge);
        graph.nodes.put(currentEdge.start.argState, currentEdge.start);
        graph.nodes.put(currentEdge.end.argState, currentEdge.end);
        graph.addToCombiEdges(currentEdge);
        currentScopeEdges.pop();
        if (!isVisitedState) {
          for (ARGState child : argState.getChildren()) {
            currentScopeEdges.push(new ScopeEdge(thisScopeNode));
            waitlist.addFirst(child);
          }
        }
      } else {
        currentEdge.irrelevantARGStates.add(argState);

        currentEdge.lastCFAEdge = argState.getChildren().stream().findFirst()
            .map(argState::getEdgesToChild).map(cfae -> cfae.get(cfae.size() - 1)).orElse(null);

        if (!argState.equals(currentEdge.start.argState)) {
          if (locstate.getLocationNode() instanceof FunctionEntryNode) {
            currentEdge.passedFunctionEntryExits
                .add(locstate.getLocationNode().getFunctionName() + " entry");
          } else if (locstate.getLocationNode() instanceof FunctionExitNode) {
            currentEdge.passedFunctionEntryExits
                .add(locstate.getLocationNode().getFunctionName() + " exit");
          }
        }
        argState.getChildren().forEach(waitlist::addFirst);
        for (int i = 0; i < argState.getChildren().size() - 1; i++) {
          currentScopeEdges.push(new ScopeEdge(currentEdge));

        }
      }

    }

    if (!currentScopeEdges.isEmpty()) {
      throw new IllegalStateException("CurrentScopeEdges not empty, this indicates a Bug!");
    }

    return graph;
  }

  public Table<ScopeNode, ScopeNode, CombiScopeEdge> computeIrrelevantLeafGraphs() {
    Table<ScopeNode, ScopeNode, CombiScopeEdge> irrelevantLeafGraphs = HashBasedTable.create();
    for (CombiScopeEdge combiScopeEdge : combiScopeEdges.values()) {
      if (!edges.containsKey(combiScopeEdge.end)) {
        irrelevantLeafGraphs.put(combiScopeEdge.start, combiScopeEdge.end, combiScopeEdge);
      }
    }
    return irrelevantLeafGraphs;
  }

  public ScopeNode getRootNode() {
    return rootNode;
  }

  public Multimap<ScopeNode, ScopeEdge> getEdges() {
    return Multimaps.unmodifiableMultimap(edges);
  }

  public Map<ARGState, ScopeNode> getNodes() {
    return nodes;
  }

  private CombiScopeEdge addToCombiEdges(ScopeEdge edge) {
    CombiScopeEdge combiEdge = combiScopeEdges.get(edge.start, edge.end);
    if (combiEdge == null) {
      combiEdge = new CombiScopeEdge(edge);
      combiScopeEdges.put(combiEdge.start, combiEdge.end, combiEdge);
    } else {
      combiEdge.scopeEdges.add(edge);
    }

    return combiEdge;
  }

  public CombiScopeEdge getCombiScopeEdge(ScopeNode start, ScopeNode end) {
    return combiScopeEdges.get(start, end);
  }

  public Collection<CombiScopeEdge> getCombiScopeEdges(ScopeNode start) {
    return Collections.unmodifiableCollection(combiScopeEdges.row(start).values());
  }

  public Collection<CombiScopeEdge> getCombiScopeEdges() {
    return Collections.unmodifiableCollection(combiScopeEdges.values());
  }


  public Collection<Reason> getScopeReasons() {
    return Collections.unmodifiableCollection(scopeReasons);
  }

  public static class ScopeNode {
    private final ARGState argState;
    private Set<Reason> scopeReasons = new LinkedHashSet<>();

    private ScopeNode(ARGState argState) {
      this.argState = argState;
    }

    public boolean isPartOfScope() {
      return scopeReasons.size() > 0;
    }

    public ARGState getArgState() {
      return argState;
    }

    public Set<Reason> getScopeReasons() {
      return Collections.unmodifiableSet(scopeReasons);
    }

    @Override
    public String toString() {
      return argState.getStateId() + "";
    }

    public String getId() {
      return Objects.toString(argState.getStateId());
    }
  }

  public static class ScopeEdge {
    private final List<String> passedFunctionEntryExits = new ArrayList<>();
    private final List<ARGState> irrelevantARGStates = new ArrayList<>();
    private ScopeNode start;
    private ScopeNode end;
    private CFAEdge lastCFAEdge;

    private ScopeEdge(ScopeNode start) {
      this.start = start;
    }

    private ScopeEdge(ScopeEdge other) {
      start = other.start;
      end = other.end;
      lastCFAEdge = other.lastCFAEdge;
      passedFunctionEntryExits.addAll(other.passedFunctionEntryExits);
      irrelevantARGStates.addAll(other.irrelevantARGStates);
    }

    public Optional<CFAEdge> getLastCFAEdge() {
      return Optional.ofNullable(lastCFAEdge);
    }

    public ScopeNode getStart() {
      return start;
    }

    public ScopeNode getEnd() {
      return end;
    }

    public List<String> getPassedFunctionEntryExits() {
      return Collections.unmodifiableList(passedFunctionEntryExits);
    }

    public int getIrrelevantARGStatesCount() {
      return irrelevantARGStates.size();
    }

    @Override
    public String toString() {
      return String
          .format("%s -%d(%s)-> %s", start, irrelevantARGStates, passedFunctionEntryExits, end);
    }

    public static class CombiScopeEdge {
      private final Set<ScopeEdge> scopeEdges = new LinkedHashSet<>();
      private final ScopeNode start;
      private final ScopeNode end;

      private CombiScopeEdge(ScopeEdge initial) {
        this(initial.getStart(), initial.getEnd());
        scopeEdges.add(initial);
      }

      private CombiScopeEdge(ScopeNode pStart, ScopeNode pEnd) {
        start = pStart;
        end = pEnd;
      }

      public Set<ScopeEdge> getScopeEdges() {
        return Collections.unmodifiableSet(scopeEdges);
      }

      public int computeIrrelevantARGStateCount() {
        return computeIrrelevantARGStates().size();
      }

      public Set<ARGState> computeIrrelevantARGStates() {
        return scopeEdges.parallelStream()
            .flatMap(edge -> edge.irrelevantARGStates.stream()).collect(Collectors.toSet());
      }

      public Set<CFAEdge> computeLastCfaEdges() {
        return scopeEdges.parallelStream()
            .map(ScopeEdge::getLastCFAEdge).filter(Optional::isPresent).map(Optional::get)
            .collect(Collectors.toSet());
      }

      public Set<List<String>> getPassedFunctionEntryExits() {
        return scopeEdges.parallelStream()
            .map(ScopeEdge::getPassedFunctionEntryExits).collect(Collectors.toSet());
      }

      public ScopeNode getStart() {
        return start;
      }

      public ScopeNode getEnd() {
        return end;
      }
    }


  }

}
