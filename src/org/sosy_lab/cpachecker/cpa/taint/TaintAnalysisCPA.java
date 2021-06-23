// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.taint.TaintAnalysisTransferRelation.TaintTransferOptions;

@Options(prefix = "cpa.taint")
public class TaintAnalysisCPA extends AbstractCPA {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for TaintAnalysisCPA")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER", "EQUALS"},
      description = "which stop operator to use for TaintAnalysisCPA")
  private String stopType = "SEP";

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final TaintTransferOptions transferOptions;

  /**
   * Gets a factory for creating PointerCPAs.
   *
   * @return a factory for creating PointerCPAs.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TaintAnalysisCPA.class);
  }

  private TaintAnalysisCPA(Configuration config, LogManager logger, CFA cfa)
      throws InvalidConfigurationException {
    super(TaintDomain.INSTANCE, null);
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    config.inject(this, TaintAnalysisCPA.class);
    logger.log(Level.INFO, "TaintAnalysis");

    transferOptions = new TaintTransferOptions(config);

    // precision = initializePrecision(config, cfa);
    // statistics = new ValueAnalysisCPAStatistics(this, config);
    // writer = new StateToFormulaWriter(config, logger, shutdownNotifier, cfa);
    // errorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(config, logger,
    // cfa.getMachineModel());

    // unknownValueHandler = createUnknownValueHandler();

    // constraintsStrengthenOperator =
    // new ConstraintsStrengthenOperator(config, logger);
    // transferOptions = new ValueTransferOptions(config);
    // precisionAdjustmentOptions = new PrecAdjustmentOptions(config, cfa);
    // precisionAdjustmentStatistics = new PrecAdjustmentStatistics();
  }

  // @Override
  // public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
  // return PointerState.INITIAL_STATE;
  // }

  @Override
  public TaintAnalysisTransferRelation getTransferRelation() {
    return new TaintAnalysisTransferRelation(logger, transferOptions);
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
    return new TaintAnalysisState(logger);
  }

  public Configuration getConfiguration() {
    return config;
  }

  public CFA getCFA() {
    return cfa;
  }

  public LogManager getLogger() {
    return logger;
  }
}
