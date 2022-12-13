// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraphBuilder;

@Options(prefix = "cpa.taint")
public final class TaintAnalysisCPA extends AbstractCPA {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "The merge operator to use for TaintAnalysisCPA.")
  private String mergeOperator = "SEP";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER", "EQUALS"},
      description = "The stop operator to use for TaintAnalysisCPA.")
  private String stopOperator = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TaintAnalysisCPA.class);
  }

  private TaintAnalysisCPA(
      Configuration pConfig, LogManager pLogger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        new TaintDomain(),
        new TaintTransferRelation(pConfig, createSdg(pConfig, pLogger, pCfa, pShutdownNotifier)));

    pConfig.inject(this);
  }

  private static CSystemDependenceGraph createSdg(
      Configuration pConfig, LogManager pLogger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          TaintAnalysisCPA.class.getSimpleName() + " only supports C");
    }

    CSystemDependenceGraphBuilder sdgBuilder =
        new CSystemDependenceGraphBuilder(pCfa, pConfig, pLogger, pShutdownNotifier);
    return sdgBuilder.build();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeOperator);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopOperator);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return TaintState.INITIAL_STATE;
  }
}
