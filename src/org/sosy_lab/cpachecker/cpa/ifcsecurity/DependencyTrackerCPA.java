// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/**
 * CPA for tracking which variables/functions are dependent on which other variables/functions
 */
@Options(prefix = "cpa.ifcsecurity")
public class DependencyTrackerCPA extends AbstractCPA {

  @SuppressWarnings("unused")
  private LogManager logger;

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for DependencyTrackerCPA")
  private String mergeType = "JOIN";

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "JOIN"},
      description="which stop operator to use for DependencyTrackerCPA")
  private String stopType = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DependencyTrackerCPA.class);
  }

  private DependencyTrackerCPA(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    super(
        DelegateAbstractDomain.<DependencyTrackerState>getInstance(),
        new DependencyTrackerRelation(pLogger, pShutdownNotifier));
    pConfig.inject(this);
    this.logger = pLogger;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    DependencyTrackerState initialstate=new DependencyTrackerState();
    return  initialstate;
  }
}
