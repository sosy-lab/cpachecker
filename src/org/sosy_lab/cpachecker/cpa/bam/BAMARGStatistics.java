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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Collections2;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.MissingBlockException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class BAMARGStatistics extends ARGStatistics {

  private final AbstractBAMCPA bamCpa;

  public BAMARGStatistics(
      Configuration pConfig,
      LogManager pLogger,
      AbstractBAMCPA pBamCpa,
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
    ARGState root = (ARGState)pReached.getFirstState();
    Collection<ARGState> targets = Collections2.filter(root.getSubgraph(),
        s -> s.getChildren().isEmpty() && !s.isCovered()
        // sometimes we find leaf-states that are at block-entry-locations,
        // and it seems that those states are "not" contained in the reachedSet.
        // I do not know the reason for this. To avoid invalid statistics, lets ignore them.
        // Possible case: entry state of an infinite loop, loop is a block without exit-state.
        // && !bamCpa.getBlockPartitioning().isCallNode(AbstractStates.extractLocation(s))
        );

    if (targets.isEmpty()) {
      if (pResult.equals(Result.FALSE)) {
        logger.log(
            Level.INFO,
            "could not compute full reached set graph (missing block), "
                + "some output or statistics might be missing");
        // invalid ARG, ignore output.
      } else if (pResult.equals(Result.TRUE)) {
        // In case of TRUE verdict we do not need a target to print super statistics
        super.printStatistics(pOut, pResult, pReached);
      }
      return;
    }

    // assertion disabled, because it happens with BAM-parallel (reason unknown).
    // assert targets.contains(pReached.getLastState()) : String.format(
    //   "Last state %s of reachedset with root %s is not in target states %s",
    //   pReached.getLastState(), pReached.getFirstState(), targets);
    assert pMainReachedSet.asReachedSet().asCollection().containsAll(targets);
    final BAMSubgraphComputer cexSubgraphComputer = new BAMSubgraphComputer(bamCpa);

    Pair<BackwardARGState, Collection<BackwardARGState>> rootAndTargetsOfSubgraph = null;
    try {
      rootAndTargetsOfSubgraph = cexSubgraphComputer.computeCounterexampleSubgraph(targets, pMainReachedSet);
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

    ARGPath path = ARGUtils.getRandomPath(rootAndTargetsOfSubgraph.getFirst());
    StatTimer dummyTimer = new StatTimer("dummy");
    BAMReachedSet bamReachedSet = new BAMReachedSet(bamCpa, pMainReachedSet, path, dummyTimer);

    UnmodifiableReachedSet bamReachedSetView = bamReachedSet.asReachedSet();

    readdCounterexampleInfo(pReached, rootAndTargetsOfSubgraph.getSecond());

    super.printStatistics(pOut, pResult, bamReachedSetView);
  }

  /**
   * This method takes the CEX-info computed last and inserts it into the current last state.
   *
   * <p>We assume that (if the last state is a target state) the last CEX-check computed a correct
   * CEX-info and wrote it into the last state of the wrapped reached-set.
   */
  private void readdCounterexampleInfo(
      UnmodifiableReachedSet pReached, Collection<BackwardARGState> targets) {
    ARGState argState = (ARGState) pReached.getLastState();
    if (argState != null && argState.isTarget()) {
      Optional<CounterexampleInfo> cex = argState.getCounterexampleInformation();
      com.google.common.base.Optional<BackwardARGState> matchingState =
          from(targets).firstMatch(t -> t.getARGState() == argState);
      if (cex.isPresent() && matchingState.isPresent()) {
        matchingState.get().addCounterexampleInformation(cex.get());
      }
    }
  }
}
