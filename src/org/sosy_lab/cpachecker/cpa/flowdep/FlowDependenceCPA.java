/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.flowdep;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
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
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerCPA;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefCPA;

/**
 * CPA that tracks the flow dependence of variable assignments.
 * A statement 'a' is flow dependent on some variable assignment 'b',
 * if 1) 'a' uses the variable x that 'b' assigns a new value to,
 * and 2) there is a program path from 'b' to 'a' without any additional
 * assignment to x.
 *
 * @see org.sosy_lab.cpachecker.util.refinement.UseDefRelation
 */
public class FlowDependenceCPA extends AbstractSingleWrapperCPA {

  private final AbstractDomain domain;
  private final FlowDependenceTransferRelation transfer;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final LogManager logger;

  private final CompositeCPA delegateCpa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FlowDependenceCPA.class);
  }

  private FlowDependenceCPA(
      final LogManager pLogger,
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCpaToWrap
  ) throws InvalidConfigurationException {
    super(pCpaToWrap);

    logger = pLogger;

    delegateCpa = (CompositeCPA) super.getWrappedCpa();

    domain = new FlowDependenceDomain();
    transfer = new FlowDependenceTransferRelation(delegateCpa.getTransferRelation(), logger);
    merge = new MergeJoinOperator(domain);
    stop = new StopJoinOperator(domain);

    if (!(pCpaToWrap instanceof CompositeCPA)) {
      throw new InvalidConfigurationException(
          FlowDependenceCPA.class.getSimpleName()
              + " requires to wrap "
              + CompositeCPA.class.getSimpleName());
    }

    for (ConfigurableProgramAnalysis cpa : ((CompositeCPA) pCpaToWrap).getWrappedCPAs()) {
      if (!(cpa instanceof ReachingDefCPA) && !(cpa instanceof PointerCPA)) {
        throw new InvalidConfigurationException(
            FlowDependenceCPA.class.getSimpleName()
                + " "
                + "requires exactly "
                + ReachingDefCPA.class.getSimpleName()
                + " and "
                + PointerCPA.class.getSimpleName());
      }
    }

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(FlowDependenceCPA.class.getSimpleName() + " only"
          + " supports C");
    }
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
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {

    return new FlowDependenceState((CompositeState) delegateCpa.getInitialState(pNode, pPartition));
  }

  @Override
  public Precision getInitialPrecision(final CFANode node, final StateSpacePartition partition)
      throws InterruptedException {
    return getWrappedCpa().getInitialPrecision(node, partition);
  }
}
