// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dominator;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * CPA that computes the dominators of CFA nodes. A CFA node `d` is a dominator of a CFA node `l` in
 * a CFA if it is part of all paths from the initial location to `l`.
 *
 * <p>This CPA can also be used for post-dominator computation, i.e., to compute all nodes that are
 * part of all paths from a given node to the program exit. To do so, run the CPA with {@link
 * org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards LocationCPABackwards}.
 *
 * <p>Note: If run with {@link org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards
 * LocationCPABackwards}, each node will be post-dominated by itself. This is not a problem and not
 * wrong, just don't be confused.
 */
public class DominatorCPA implements ConfigurableProgramAnalysis {

  private AbstractDomain abstractDomain;
  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DominatorCPA.class);
  }

  public DominatorCPA() {
    abstractDomain = DelegateAbstractDomain.getInstance();
    transferRelation = new DominatorTransferRelation();
    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
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
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new DominatorState(ImmutableSet.of(pNode));
  }
}
