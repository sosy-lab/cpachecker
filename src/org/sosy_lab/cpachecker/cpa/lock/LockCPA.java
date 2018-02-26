/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

@Options(prefix = "cpa.lock")
public class LockCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {

  public static enum LockAnalysisMode {
    RACE,
    DEADLOCK
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LockCPA.class);
  }

  @Option(
    name = "analysisMode",
    description = "What are we searching for: race or deadlock",
    secure = true
  )
  private LockAnalysisMode analysisMode = LockAnalysisMode.RACE;

  private final Reducer reducer;

  private LockCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(
        "sep",
        "sep",
        DelegateAbstractDomain.<AbstractLockState>getInstance(),
        new LockTransferRelation(config, logger));
    config.inject(this);
    switch (analysisMode) {
      case RACE:
        reducer = new LockReducer(config);
        break;

      case DEADLOCK:
        reducer = NoOpReducer.getInstance();
        break;

      default:
        throw new InvalidConfigurationException("Unknown mode: " + analysisMode);
    }
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition pPartition) {
    switch (analysisMode) {
      case RACE:
        return new LockState();

      case DEADLOCK:
        return new DeadLockState();

      default:
        // The analysis should fail at CPA creation
        throw new UnsupportedOperationException("Unsupported analysis mode");
    }
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    LockTransferRelation transfer = (LockTransferRelation) getTransferRelation();
    pStatsCollection.add(transfer.getStatistics());
  }
}
