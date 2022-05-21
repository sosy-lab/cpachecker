// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage.analysisindependent;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonAbstractState;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageCPA;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

/**
 * Calculates TDCG and Coverage Measure data during the analysis. This CPA depends heavily on
 * util.coverage package.
 */
public class AnalysisIndependentCoverageCPA implements CoverageCPA {
  private final TransferRelation transfer;
  private final AbstractDomain domain;
  private final StopOperator stop;
  private final CoverageCollectorHandler coverageCollectorHandler;

  public AnalysisIndependentCoverageCPA(CoverageCollectorHandler pCovCollectorHandler) {
    coverageCollectorHandler = pCovCollectorHandler;
    domain = new FlatLatticeDomain(SingletonAbstractState.INSTANCE);
    stop = new StopSepOperator(domain);
    transfer = new AnalysisIndependentCoverageTransferRelation(coverageCollectorHandler);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AnalysisIndependentCoverageCPA.class);
  }

  @Override
  public CoverageCollectorHandler getCoverageCollectorHandler() {
    return coverageCollectorHandler;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return SingletonAbstractState.INSTANCE;
  }
}
