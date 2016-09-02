/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

import java.util.Collection;

public class PropertyScopeCPA extends AbstractCPA implements StatisticsProvider{

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  protected PropertyScopeCPA(Configuration config, LogManager logger,
                             CFA pCfa, ShutdownNotifier pShutdownNotifier) {
    super("sep", "sep", new FlatLatticeDomain(), new PropertyScopeTransferRelation());
    this.config = config;
    this.logger = logger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new PropertyScopePrecisionAdjustment();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PropertyScopeCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return new PropertyScopeState();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {

  }
}
