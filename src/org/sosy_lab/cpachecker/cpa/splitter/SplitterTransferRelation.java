/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.splitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
  @ClassOption(packagePrefix = {"org.sosy_lab.cpachecker.cpa.splitter.heuristics"})
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
        Collection<SplitInfoState> successors = new ArrayList<>(numParts);
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

        if (!successors.isEmpty()) {
          logger.log(Level.FINE, "Divided split indices.");
          return successors;
        }
      }
    }

    return Collections.singleton(splitState);
  }
}
