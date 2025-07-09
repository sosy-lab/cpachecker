// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisTransferRelation.PointerTransferOptions;

@Options(prefix = "cpa.pointer")
public class PointerAnalysisCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"JOIN", "SEP"},
      description = "which merge operator to use for PointerAnalysisCPA")
  private String mergeType = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"JOIN", "SEP"},
      description = "which stop operator to use for PointerAnalysisCPA")
  private String stopType = "SEP";

  private final LogManager logger;
  private final PointerTransferOptions transferOptions;

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new PointerAnalysisState();
  }

  public PointerAnalysisCPA(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(DelegateAbstractDomain.getInstance(), null);
    logger = pLogger;
    pConfig.inject(this, PointerAnalysisCPA.class);
    transferOptions = new PointerTransferOptions(pConfig);
  }

  @Override
  public PointerAnalysisTransferRelation getTransferRelation() {
    return new PointerAnalysisTransferRelation(logger, transferOptions);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PointerAnalysisCPA.class);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }
}
