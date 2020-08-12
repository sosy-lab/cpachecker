/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerCPA;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerCPA.PointerOptions;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerReducer;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;

public class RCUSearchCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM,
                                                         StatisticsProvider, WrapperCPA {

  private final RCUSearchStatistics statistics;
  private final PointerCPA pointerCPA;
  private final RCUSearchReducer reducer;

  RCUSearchCPA (Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(
        "JOIN",
        "SEP",
        DelegateAbstractDomain.<RCUSearchState>getInstance(),
        new RCUSearchTransfer(config, pLogger));
    statistics = new RCUSearchStatistics(config, pLogger);

    PointerOptions options = new PointerOptions();
    config.inject(options);
    pointerCPA = new PointerCPA(options);
    reducer = new RCUSearchReducer((PointerReducer) pointerCPA.getReducer(), statistics);
    ((RCUSearchTransfer) getTransferRelation())
        .initialize((PointerTransferRelation) pointerCPA.getTransferRelation(), statistics);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RCUSearchCPA.class);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new RCUSearchState();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
    pointerCPA.collectStatistics(statsCollection);
  }

  @Nullable
  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> type) {
    if (type.isAssignableFrom(getClass())){
      return type.cast(this);
    }

    if (type.isAssignableFrom(pointerCPA.getClass())){
      return type.cast(pointerCPA);
    }

    return null;
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return Collections.singleton(pointerCPA);
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }
}
