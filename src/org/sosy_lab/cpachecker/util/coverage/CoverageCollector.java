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
package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Class responsible for extracting coverage information.
 */
public abstract class CoverageCollector {
  static public CoverageCollector fromReachedSet(UnmodifiableReachedSet pReachedSet, CFA cfa) {
    return new ReachedSetCoverageCollector(pReachedSet, cfa);
  }
  static public CoverageCollector fromCounterexample(ARGPath pPath) {
    return new CounterexampleCoverageCollector(pPath);
  }

  public Map<String, FileCoverageInformation> collectCoverage() {
    // Add information about existing functions
    Map<String, FileCoverageInformation> infosPerFile = collectExistingFunctions();

    // Add information about existing locations
    for (CFAEdge e : collectExistingEdges()) {
      handleExistedEdge(e, infosPerFile);
    }

    // Add information about covered locations
    for (CFAEdge e : collectCoveredEdges()) {
      handleCoveredEdge(e, infosPerFile);
    }
    return infosPerFile;
  }

  protected abstract Multiset<CFAEdge> collectExistingEdges();
  protected abstract Map<String, FileCoverageInformation> collectExistingFunctions();
  protected abstract Multiset<CFAEdge> collectCoveredEdges();

  private void handleExistedEdge(
      final CFAEdge pEdge,
      final Map<String, FileCoverageInformation> pCollectors) {

    final FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge)pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return;
    }

    final FileCoverageInformation collector = getFileInfoTarget(loc, pCollectors);

    if (pEdge instanceof AssumeEdge) {
      collector.addExistingAssume((AssumeEdge) pEdge);
    }

    //Do not extract lines from edge - there are not origin lines
    collector.addExistingLine(loc.getStartingLineInOrigin());
  }

  public static boolean coversLine(CFAEdge pEdge) {
    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      return false;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge)pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      return false;
    }
    return true;
  }
  private void handleCoveredEdge(
      final CFAEdge pEdge,
      final Map<String, FileCoverageInformation> pCollectors) {

    FileLocation loc = pEdge.getFileLocation();
    if (!coversLine(pEdge)) {
      return;
    }

    final FileCoverageInformation collector = getFileInfoTarget(loc, pCollectors);

    if (pEdge instanceof AssumeEdge) {
      collector.addVisitedAssume((AssumeEdge) pEdge);
    }

    //Do not extract lines from edge - there are not origin lines
    collector.addVisitedLine(loc.getStartingLineInOrigin());
  }

  protected FileCoverageInformation getFileInfoTarget(
      final FileLocation pLoc,
      final Map<String, FileCoverageInformation> pTargets) {

    assert pLoc.getStartingLineNumber() != 0; // Cannot produce coverage info for dummy file location

    String file = pLoc.getFileName();
    FileCoverageInformation fileInfos = pTargets.get(file);

    if (fileInfos == null) {
      fileInfos = new FileCoverageInformation();
      pTargets.put(file, fileInfos);
    }

    return fileInfos;
  }

  protected Multiset<FunctionEntryNode> getFunctionEntriesFromReached(UnmodifiableReachedSet pReached) {
    if (pReached instanceof ForwardingReachedSet) {
      pReached = ((ForwardingReachedSet) pReached).getDelegate();
    }
    return HashMultiset.create(from(pReached)
                .transform(EXTRACT_LOCATION)
                .filter(notNull())
                .filter(FunctionEntryNode.class)
                .toList());
  }

}

class CounterexampleCoverageCollector extends CoverageCollector {
  final private ARGPath cexPath;
  private Optional<HashMultiset<CFAEdge>> coveredEdges;
  public CounterexampleCoverageCollector(ARGPath pCexPath) {
    cexPath = pCexPath;
    coveredEdges = Optional.empty();
  }

  private static boolean isOutsideAssumptionAutomaton(ARGState s) {
    boolean foundAssumptionAutomaton = false;
    for (AutomatonState aState : AbstractStates.asIterable(s).filter(AutomatonState.class)) {
      if (aState.getOwningAutomatonName().equals("AssumptionAutomaton")) {
        foundAssumptionAutomaton = true;
        if (aState.getInternalStateName().equals("__FALSE")) {
          return true;
        }
      }
    }
    if (!foundAssumptionAutomaton) {
      throw new IllegalArgumentException(
          "This method should only be called when an " +
          "Assumption Automaton is used as part of the specification.");
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.coverage.CoverageCollector#collectExistingEdges()
   *
   * Coverage from a counterexample does not report all existing edges, but the set
   * of existing edges needs to contain all covered edges at the minimum.
   */
  @Override
  protected Multiset<CFAEdge> collectExistingEdges() {
    return collectCoveredEdges();
  }

  @Override
  protected Multiset<CFAEdge> collectCoveredEdges() {
    if (!coveredEdges.isPresent()) {
      coveredEdges = Optional.of(HashMultiset.create());
      PathIterator pathIterator = cexPath.fullPathIterator();
      while(pathIterator.hasNext()) {
        CFAEdge edge = pathIterator.getOutgoingEdge();

        // Considering covered up until (but not including) when the
        // AssumptionAutomaton state is __FALSE.
        if (isOutsideAssumptionAutomaton(pathIterator.getNextAbstractState())) {
          break;
        }
        coveredEdges.get().add(edge);
        pathIterator.advance();
      }
    }
    return coveredEdges.get();
  }

  @Override
  protected Map<String, FileCoverageInformation> collectExistingFunctions() {
    return new HashMap<>();
  }
}
class ReachedSetCoverageCollector extends CoverageCollector {
  final private UnmodifiableReachedSet reached;
  final private CFA cfa;
  public ReachedSetCoverageCollector(UnmodifiableReachedSet pReachedSet, CFA pCfa) {
    reached = pReachedSet;
    cfa = pCfa;
  }

  @Override
  protected Multiset<CFAEdge> collectExistingEdges() {
    Multiset<CFAEdge> existingEdges = HashMultiset.create();
    for (CFANode node : cfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        existingEdges.add(node.getLeavingEdge(i));
      }
    }
    return existingEdges;
  }

  @Override
  protected Multiset<CFAEdge> collectCoveredEdges() {
    Multiset<CFAEdge> coveredEdges = HashMultiset.create();
    Set<CFANode> reachedNodes = from(reached)
        .transform(EXTRACT_LOCATION)
        .filter(notNull())
        .toSet();
    //Add information about visited locations

    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null ) {
        for (ARGState child : argState.getChildren()) {
          // Do not specially check child.isCovered, as the edge to covered state also should be marked as covered edge
          List<CFAEdge> edges = argState.getEdgesToChild(child);
          if (edges.size() > 1) {
            for (CFAEdge innerEdge : edges) {
              coveredEdges.add(innerEdge);
            }

            //BAM produces paths with no edge connection thus the list will be empty
          } else if (!edges.isEmpty()) {
            coveredEdges.add(Iterables.getOnlyElement(edges));
          }
        }
      } else {
        //Simple kind of analysis
        //Cover all edges from reached nodes
        //It is less precise, but without ARG it is impossible to know what path we chose
        CFANode node = AbstractStates.extractLocation(state);
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          if (reachedNodes.contains(edge.getSuccessor())) {
            coveredEdges.add(edge);
          }
        }
      }
    }
    return coveredEdges;
  }

  @Override
  protected Map<String, FileCoverageInformation> collectExistingFunctions() {
    Multiset<FunctionEntryNode> reachedLocations = getFunctionEntriesFromReached(reached);
    Map<String, FileCoverageInformation> infosPerFile = new HashMap<>();

    // Add information about existing functions
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      final FileLocation loc = entryNode.getFileLocation();
      if (loc.getStartingLineNumber() == 0) {
        // dummy location
        continue;
      }
      final String functionName = entryNode.getFunctionName();
      final FileCoverageInformation infos = getFileInfoTarget(loc, infosPerFile);

      final int startingLine = loc.getStartingLineInOrigin();
      final int endingLine = loc.getEndingLineInOrigin();

      infos.addExistingFunction(functionName, startingLine, endingLine);

      if (reachedLocations.contains(entryNode)) {
        infos.addVisitedFunction(entryNode.getFunctionName(), reachedLocations.count(entryNode));
      }
    }
    return infosPerFile;
  }
}