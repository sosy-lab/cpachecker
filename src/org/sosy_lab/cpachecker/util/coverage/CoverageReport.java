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
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible for extracting coverage information from ReachedSet and CFA
 * and writing it into a file.
 */
@Options
public class CoverageReport {

  @Option(secure=true,
      name="coverage.enabled",
      description="Compute and export information about the verification coverage?")
  private boolean enabled = true;

  private final Collection<CoverageWriter> reportWriters;

  public CoverageReport(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.reportWriters = Lists.newArrayList();
    this.reportWriters.add(new CoverageReportGcov(pConfig, pLogger));
    this.reportWriters.add(new CoverageReportStdoutSummary(pConfig));

  }

  public void writeCoverageReport(
      final PrintStream pStatisticsOutput,
      final ReachedSet pReached,
      final CFA pCfa) {

    if (!enabled) {
      return;
    }

    Multiset<FunctionEntryNode> reachedLocations = getFunctionEntriesFromReached(pReached);

    Map<String, FileCoverageInformation> infosPerFile = new HashMap<>();

    // Add information about existing functions
    for (FunctionEntryNode entryNode : pCfa.getAllFunctionHeads()) {
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

    //Add information about existing locations
    for (CFANode node : pCfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        handleExistedEdge(node.getLeavingEdge(i), infosPerFile);
      }
    }

    Set<CFANode> reachedNodes = from(pReached)
                                .transform(EXTRACT_LOCATION)
                                .filter(notNull())
                                .toSet();
    //Add information about visited locations
    for (AbstractState state : pReached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null ) {
        for (ARGState child : argState.getChildren()) {
          if (!child.isCovered()) {
            List<CFAEdge> edges = argState.getEdgesToChild(child);
            if (edges.size() > 1) {
              for (CFAEdge innerEdge : edges) {
                handleCoveredEdge(innerEdge, infosPerFile);
              }

              //BAM produces paths with no edge connection thus the list will be empty
            } else if (!edges.isEmpty()) {
              handleCoveredEdge(Iterables.getOnlyElement(edges), infosPerFile);
            }
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
            handleCoveredEdge(edge, infosPerFile);
          }
        }
      }
    }

    for (CoverageWriter w: reportWriters) {
      w.write(infosPerFile, pStatisticsOutput);
    }

  }

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

    collector.addExistingLine(pEdge.getLineNumber());
  }


  private void handleCoveredEdge(
      final CFAEdge pEdge,
      final Map<String, FileCoverageInformation> pCollectors) {

    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      return;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge)pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      return;
    }

    final FileCoverageInformation collector = getFileInfoTarget(loc, pCollectors);

    if (pEdge instanceof AssumeEdge) {
      collector.addVisitedAssume((AssumeEdge) pEdge);
    }

    collector.addVisitedLine(pEdge.getLineNumber());
  }

  private FileCoverageInformation getFileInfoTarget(
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

  private Multiset<FunctionEntryNode> getFunctionEntriesFromReached(ReachedSet pReached) {
    if (pReached instanceof ForwardingReachedSet) {
      pReached = ((ForwardingReachedSet)pReached).getDelegate();
    }
    return HashMultiset.create(from(pReached)
                .transform(EXTRACT_LOCATION)
                .filter(notNull())
                .filter(FunctionEntryNode.class)
                .toList());
  }

}
