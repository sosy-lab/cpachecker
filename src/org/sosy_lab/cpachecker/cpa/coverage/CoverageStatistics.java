/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.coverage;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.CoverageMode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

@Options
public class CoverageStatistics extends AbstractStatistics {

  @Option(secure=true, name="coverage.stdout",
      description="print coverage summary to stdout")
  private boolean writeToStdout = true;

  @Option(secure=true, name="coverage.export",
      description="print coverage info to file")
  private boolean writeToFile = true;

  @Option(secure=true, name="coverage.file",
      description="print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.info");

  private final LogManager logger;
  private final CoverageData cov;
  private final CFA cfa;

  public CoverageStatistics(Configuration pConfig, LogManager pLogger, CFA pCFA, CoverageData pCov)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
    this.cov = pCov;
    this.cfa = pCFA;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

    if (cov.getCoverageMode() == CoverageMode.REACHED) {
      computeCoverageFromReached(pReached);
    }

    if (writeToStdout) {
      CoverageReportStdoutSummary writer = new CoverageReportStdoutSummary();
      writer.write(cov, pOut);
    }

    if (writeToFile && outputCoverageFile != null) {
      CoverageReportGcov writer = new CoverageReportGcov(logger);
      writer.write(cov, outputCoverageFile);
    }

  }

  @Override
  public String getName() {
    return String.format("Code Coverage (Mode: %s)", cov.getCoverageMode().toString());
  }

  public void computeCoverageFromReached(
      final UnmodifiableReachedSet pReached) {

    Set<CFANode> reachedLocations = getAllLocationsFromReached(pReached);

    //Add information about visited locations
    for (CFANode node : cfa.getAllNodes()) {
       //This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        boolean visited = reachedLocations.contains(edge.getPredecessor())
            && reachedLocations.contains(edge.getSuccessor());

        cov.handleEdgeCoverage(edge, visited);
      }
    }

    // Add information about visited functions
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      if (cov.putExistingFunction(entryNode)) {
        if (reachedLocations.contains(entryNode)) {
          cov.addVisitedFunction(entryNode);
        }
      }
    }

  }

  private Set<CFANode> getAllLocationsFromReached(UnmodifiableReachedSet pReached) {
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
