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
package org.sosy_lab.cpachecker.cpa.rcucpa.cpalias;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

public class AliasCPA extends AbstractCPA implements ConfigurableProgramAnalysis,
                                                     StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AliasCPA.class);
  }

  private final CFA cfa;
  private final LogManager log;
  private final AliasStatistics stats;

  protected AliasCPA(Configuration config, LogManager log, CFA cfa)
      throws InvalidConfigurationException {
    super("JOIN", "SEP", DelegateAbstractDomain.<AliasState>getInstance(),
          new AliasTransfer(config, log));
    this.log = log;
    this.cfa = cfa;
    this.stats = new AliasStatistics();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator("JOIN");
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator("SEP");
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new AliasState(new HashMap<>(), new HashMap<>(), new HashSet<>());
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new AliasPrecision(new HashSet<>());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }
}
