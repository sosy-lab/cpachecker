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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertyScopeGraph {

  private final ScopeNode rootNode;
  private final Multimap<ScopeNode, ScopeEdge> edges = LinkedHashMultimap.create();
  private final Collection<Reason> scopeReasons;

  private PropertyScopeGraph(ScopeNode rootNode, Collection<Reason> pScopeReasons) {
    this.rootNode = rootNode;
    scopeReasons = pScopeReasons;
  }

  public static PropertyScopeGraph create(ARGState root, Collection<Reason> pScopeReasons) {
    PropertyScopeGraph graph = new PropertyScopeGraph(new ScopeNode(root), pScopeReasons);
    ScopeEdge currentEdge = null;

    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.addFirst(root);
    while (!waitlist.isEmpty()) {
      ARGState argState = waitlist.removeFirst();
      CallstackState csState = extractStateByType(argState, CallstackState.class);
      PropertyScopeState psState = extractStateByType(argState, PropertyScopeState.class);
      LocationState locstate = extractStateByType(argState, LocationState.class);

      Set<ScopeLocation> scopeLocations = psState.getScopeLocations().stream()
          .filter(sloc -> pScopeReasons.contains(sloc.getReason()))
          .collect(Collectors.toSet());

      ScopeNode thisScopeNode = new ScopeNode(argState);
      scopeLocations.forEach(sloc -> thisScopeNode.scopeReasons.add(sloc.getReason()));

      if (currentEdge == null) {
        currentEdge = new ScopeEdge();
        currentEdge.start = thisScopeNode;
      }

      if (locstate.getLocationNode() instanceof FunctionEntryNode) {
        currentEdge.passedFunctions.add(locstate.getLocationNode().getFunctionName());
      }

      if (argState.getChildren().size() != 1 || !scopeLocations.isEmpty()) {
        currentEdge.end = thisScopeNode;
        graph.edges.put(currentEdge.start, currentEdge);
        currentEdge = null;

      } else {
        currentEdge.irrelevantARGStates += 1;
      }

      argState.getChildren().forEach(waitlist::addFirst);
    }

    return graph;
  }

  public ScopeNode getRootNode() {
    return rootNode;
  }

  public Multimap<ScopeNode, ScopeEdge> getEdges() {
    return Multimaps.unmodifiableMultimap(edges);
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


  }

  public static class ScopeEdge {
    private ScopeNode start;
    private ScopeNode end;
    private List<String> passedFunctions = new ArrayList<>();
    private int irrelevantARGStates = 0;

    private ScopeEdge() {

    }

    public ScopeNode getStart() {
      return start;
    }

    public ScopeNode getEnd() {
      return end;
    }

    public List<String> getPassedFunctions() {
      return Collections.unmodifiableList(passedFunctions);
    }

    public int getIrrelevantARGStates() {
      return irrelevantARGStates;
    }

    @Override
    public String toString() {
      return String.format("%s -%d(%s)-> %s", start, irrelevantARGStates, passedFunctions, end);
    }
  }

}
