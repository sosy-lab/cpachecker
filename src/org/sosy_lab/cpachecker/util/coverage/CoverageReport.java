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

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.collect.Lists;

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
    this.reportWriters.add(new CoverageReportStdoutSummary(pConfig, pLogger));

  }

  public void writeCoverageReport(
      final PrintStream pStatisticsOutput,
      final Result pResult,
      final ReachedSet pReached,
      final CFA pCfa) {

    if (!enabled) {
      return;
    }

    Set<CFANode> reachedLocations = getAllLocationsFromReached(pReached);

    Map<String, FileCoverageInformation> infosPerFile = new HashMap<>();

    //Add information about visited locations
    for (CFANode node : pCfa.getAllNodes()) {
       //This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        boolean visited = reachedLocations.contains(edge.getPredecessor())
            && reachedLocations.contains(edge.getSuccessor());

        if (edge instanceof MultiEdge) {
          for (CFAEdge innerEdge : ((MultiEdge)edge).getEdges()) {
            handleEdgeCoverage(innerEdge, visited, infosPerFile);
          }
        } else {
          handleEdgeCoverage(edge, visited, infosPerFile);
        }
      }
    }

    // Add information about visited functions
    for (FunctionEntryNode entryNode : pCfa.getAllFunctionHeads()) {
      final FileLocation loc = entryNode.getFileLocation();
      if (loc.getStartingLineNumber() == 0) {
        // dummy location
        continue;
      }
      final String functionName = entryNode.getFunctionName();
      final FileCoverageInformation infos = getFileInfoTarget(loc, infosPerFile);

      final int startingLine = loc.getStartingLineInOrigin();
      final int endingLine = loc.getEndingLineNumber() - loc.getStartingLineNumber() + loc.getStartingLineInOrigin();

      infos.addExistingFunction(functionName, startingLine, endingLine);

      if (reachedLocations.contains(entryNode)) {
        infos.addVisitedFunction(functionName);
      }
    }

    for (CoverageWriter w: reportWriters) {
      w.write(infosPerFile, pStatisticsOutput);
    }

  }

  private void handleEdgeCoverage(
      final CFAEdge pEdge,
      final boolean pVisited,
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

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineNumber() - loc.getStartingLineNumber() + loc.getStartingLineInOrigin();

    for (int line = startingLine; line <= endingLine; line++) {
      collector.addExistingLine(line);
    }

    if (pVisited) {
      for (int line = startingLine; line <= endingLine; line++) {
        collector.addVisitedLine(line);
      }
    }
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

  private Set<CFANode> getAllLocationsFromReached(ReachedSet pReached) {
    if (pReached instanceof ForwardingReachedSet) {
      pReached = ((ForwardingReachedSet)pReached).getDelegate();
    }

    if (pReached instanceof LocationMappedReachedSet) {
      return ((LocationMappedReachedSet)pReached).getLocations();

    } else {
      return from(pReached)
                  .transform(EXTRACT_LOCATION)
                  .filter(notNull())
                  .toSet();
    }
  }

}
