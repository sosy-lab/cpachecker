/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.io.PrintStream;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.MissingBlockException;

public class BAMARGStatistics extends ARGStatistics {

  private final BAMCPA bamCpa;

  public BAMARGStatistics(
      Configuration pConfig,
      LogManager pLogger,
      BAMCPA pBamCpa,
      ConfigurableProgramAnalysis pCpa,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pSpecification, pCfa);
    bamCpa = pBamCpa;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.WARNING, "statistic export needs ARG-CPA");
      return; // invalid CPA, nothing to do
    }

    if (pReached.size() <= 1) {
      // interrupt, timeout -> no CEX available, ignore reached-set
      logger.log(Level.WARNING, "could not compute full reached set graph, there is no exit state");
      return;
    }

    // create pseudo-reached-set for export.
    // it will be sufficient for exporting a CEX (error-path, error-witness, harness)

    // TODO create 'full' reached-set to export correctness witnesses.
    // This might cause a lot of overhead, because of missing blocks,
    // aggressive caching, and multi-usage of blocks.

    ARGReachedSet pMainReachedSet =
        new ARGReachedSet((ReachedSet) pReached, (ARGCPA) cpa, 0 /* irrelevant number */);
    ARGState target = (ARGState) pReached.getLastState();
    assert pMainReachedSet.asReachedSet().contains(target);
    final BAMCEXSubgraphComputer cexSubgraphComputer = new BAMCEXSubgraphComputer(bamCpa);

    ARGState rootOfSubgraph = null;
    try {
      rootOfSubgraph = cexSubgraphComputer.computeCounterexampleSubgraph(target, pMainReachedSet);
    } catch (MissingBlockException e) {
      logger.log(
          Level.INFO,
          "could not compute full reached set graph (missing block), "
              + "some output or statistics might be missing");
      return; // invalid ARG, ignore output.

    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "could not compute full reached set graph:", e);
      return; // invalid ARG, ignore output
    }

    ARGPath path = ARGUtils.getRandomPath(rootOfSubgraph);
    Timer dummyTimer = new Timer();
    BAMReachedSet bamReachedSet =
        new BAMReachedSet(bamCpa, pMainReachedSet, path, rootOfSubgraph, dummyTimer);

    super.printStatistics(pOut, pResult, bamReachedSet.asReachedSet());
  }
}
