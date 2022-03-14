// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.splitter;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.splitter.heuristics.SplitAtAssumes;
import org.sosy_lab.cpachecker.cpa.splitter.heuristics.SplitHeuristic;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "program.splitter")
public class SplitterTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure = true, name = "heuristic", description = "Which program split heuristic to use")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cpa.splitter.heuristics")
  private SplitHeuristic.Factory factory = (pConfig, pLogger, pMaxSplits) -> new SplitAtAssumes();

  private final SplitHeuristic split;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;

  public SplitterTransferRelation(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final int pMaxSplits)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    shutdown = pShutdownNotifier;

    if (factory == null) {
      throw new InvalidConfigurationException("Heuristic for program splitting must be defined");
    }
    split = factory.create(pConfig, pLogger, pMaxSplits);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    SplitInfoState splitState = (SplitInfoState) pState;

    if (split.removeSplitIndices(pCfaEdge)) {
      splitState = splitState.removeFromSplit(split.getIndicesToRemove(pCfaEdge));
    }

    shutdown.shutdownIfNecessary();

    if (split.divideSplitIndices(pCfaEdge)) {
      int numParts = split.divideIntoHowManyParts(pCfaEdge);
      if (numParts > 1) {
        int start, end;
        ImmutableList.Builder<SplitInfoState> successors =
            ImmutableList.builderWithExpectedSize(numParts);
        SplitInfoState successor;
        if (pCfaEdge instanceof AssumeEdge) {
          AssumeEdge assume = (AssumeEdge) pCfaEdge;
          if (assume.getTruthAssumption()) {
            start = 0;
            end = numParts / 2;
          } else {
            start = numParts / 2;
            end = numParts;
          }
        } else {
          start = 0;
          end = numParts;
        }

        for (int i = start; i < end; i++) {
          successor = splitState.getSplitPart(numParts, i);
          if (successor != splitState) {
            successors.add(successor);
          }
        }

        ImmutableList<SplitInfoState> result = successors.build();
        if (!result.isEmpty()) {
          logger.log(Level.FINE, "Divided split indices.");
          return result;
        }
      }
    }

    return ImmutableList.of(splitState);
  }
}
