// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisTransferRelation;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

@Options(prefix = "cpa.globalvar")
public class GlobalVarAnalysisCPA extends AbstractCPA {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for GlobalVarAnalysisCPA")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER"},
      description = "which stop operator to use for GlobalVarAnalysisCPA")
  private String stopType = "SEP";

  private final LogManager logger;
  private final StateToFormulaWriter writer;

  private GlobalVarAnalysisCPA(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier,
      CFA cfa) throws InvalidConfigurationException {
    super(
        "sep",
        "sep",
        new FlatLatticeDomain(),
        new GlobalVarAnalysisTransferRelation(pLogger));
    config.inject(this);
    logger = pLogger;
    writer = new StateToFormulaWriter(config, pLogger, shutdownNotifier, cfa);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(GlobalVarAnalysisCPA.class);
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
    return new GlobalVarAnalysisTransferRelation(logger);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new GlobalVarAnalysisState();
  }
}
