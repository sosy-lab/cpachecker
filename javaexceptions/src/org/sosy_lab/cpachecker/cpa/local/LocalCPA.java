// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.local;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

@Options(prefix = "cpa.local")
public class LocalCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {
  private Statistics statistics;

  @Option(name = "localvariables", description = "variables, which are always local", secure = true)
  private Set<String> localVariables = ImmutableSet.of();

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LocalCPA.class);
  }

  private LocalCPA(LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException {
    super(
        "join",
        "sep",
        DelegateAbstractDomain.<LocalState>getInstance(),
        new LocalTransferRelation(pConfig));
    pConfig.inject(this);
    statistics = new LocalStatistics(pConfig, pLogger);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition p) {
    return LocalState.createInitialLocalState(localVariables);
  }

  @Override
  public Reducer getReducer() {
    return new LocalReducer();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
