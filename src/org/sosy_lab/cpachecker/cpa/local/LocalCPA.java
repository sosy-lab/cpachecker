/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.local;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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

public class LocalCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM, StatisticsProvider {
    private Statistics statistics;
    private final Reducer reducer;

    public static Set<String> localVariables;

    public static CPAFactory factory() {
      return AutomaticCPAFactory.forType(LocalCPA.class);
    }

    private LocalCPA(LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException {
      super("join", "sep", DelegateAbstractDomain.<LocalState>getInstance(), new LocalTransferRelation(pConfig));
      statistics = new LocalStatistics(pConfig, pLogger);
      reducer = new LocalReducer();
      @SuppressWarnings("deprecation")
      String localVars = pConfig.getProperty("cpa.local.localvariables");
      if (localVars != null) {
        localVariables = new HashSet<>(Arrays.asList(localVars.split(", ")));
      } else {
        localVariables = Collections.emptySet();
      }
    }

    @Override
    public AbstractState getInitialState(CFANode pNode, StateSpacePartition p) {
      return new LocalState(null);
    }

    @Override
    public Reducer getReducer() {
      return reducer;
    }

    @Override
    public void collectStatistics(Collection<Statistics> pStatsCollection) {
      pStatsCollection.add(statistics);
    }
}
