// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class WitnessJoinerCPA extends AbstractSingleWrapperCPA {

  private final StopOperator stop;
  private final TransferRelation transfer;
  private final AbstractDomain domain;
  private final MergeOperator merge;
  private final PrecisionAdjustment precAdjust;

  protected WitnessJoinerCPA(final ConfigurableProgramAnalysis pCpa) {
    super(pCpa);
    domain = new WitnessJoinerDomain(pCpa.getAbstractDomain());
    StopOperator wrappedStop = pCpa.getStopOperator();
    if (wrappedStop instanceof StopSepOperator) {
      stop = new StopSepOperator(domain);
    } else {
      stop = new WitnessJoinerStopOperator(wrappedStop);
    }
    MergeOperator wrappedMerge = pCpa.getMergeOperator();
    if (wrappedMerge == MergeSepOperator.getInstance()
        || wrappedMerge instanceof MergeSepOperator) {
      merge = MergeSepOperator.getInstance();
    } else if (wrappedMerge instanceof MergeJoinOperator) {
      merge = new MergeJoinOperator(domain);
    } else {
      merge = new WitnessJoinerMergeOperator(wrappedMerge);
    }
    transfer = new WitnessJoinerTransferRelation(pCpa.getTransferRelation());
    precAdjust = new WitnessJoinerPrecisionAdjustment(pCpa.getPrecisionAdjustment());
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(WitnessJoinerCPA.class);
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
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precAdjust;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new WitnessJoinerState(getWrappedCpa().getInitialState(node, partition));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return getWrappedCpa().getInitialPrecision(pNode, pPartition);
  }
}
