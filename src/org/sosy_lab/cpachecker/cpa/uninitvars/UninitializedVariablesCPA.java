// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix = "cpa.uninitvars")
public class UninitializedVariablesCPA extends AbstractCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UninitializedVariablesCPA.class);
  }

  @Option(
      secure = true,
      description = "print warnings during analysis when uninitialized variables are used")
  private String printWarnings = "true";

  @Option(
      secure = true,
      name = "merge",
      values = {"sep", "join"},
      description = "which merge operator to use for UninitializedVariablesCPA?")
  private String mergeType = "sep";

  @Option(
      secure = true,
      name = "stop",
      values = {"sep", "join"},
      description = "which stop operator to use for UninitializedVariablesCPA?")
  private String stopType = "sep";

  private final UninitializedVariablesStatistics statistics;

  private UninitializedVariablesCPA(Configuration config) throws InvalidConfigurationException {
    super(new UninitializedVariablesDomain(), null);
    config.inject(this);
    statistics = new UninitializedVariablesStatistics(printWarnings);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new UninitializedVariablesState(pNode.getFunctionName());
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
  public TransferRelation getTransferRelation() {
    return new UninitializedVariablesTransferRelation(printWarnings);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
