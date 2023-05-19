// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.nondeterminism;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismAbstractionState;

public class NondeterminismCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(NondeterminismCPA.class);
  }

  private final DelegateAbstractDomain<NondeterminismState> domain;

  private final NondeterminismTransferRelation transferRelation;

  private final MergeOperator mergeOperator = new NondeterminismMergeOperator();

  @Options(prefix = "cpa.nondeterminism")
  private static class NondeterminismOptions {

    @Option(
        secure = true,
        description =
            "keep tracking nondeterministically-assigned variables even if they are used in"
                + " assumptions")
    private boolean acceptConstrained = true;
  }

  public NondeterminismCPA(CFA pCFA, Configuration pConfig) throws InvalidConfigurationException {
    domain = DelegateAbstractDomain.<NondeterminismState>getInstance();
    NondeterminismOptions options = new NondeterminismOptions();
    pConfig.inject(options);
    transferRelation = new NondeterminismTransferRelation(pCFA, options.acceptConstrained);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new NondeterminismAbstractionState(ImmutableSet.of());
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return NondeterminismPrecisionAdjustment.INSTANCE;
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(domain);
  }
}
