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
package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeNoTopDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.ArithmeticOverflowAssumptionBuilder;

/**
 * CPA for detecting overflows in C programs.
 */
public class OverflowCPA
    extends SingleEdgeTransferRelation
    implements ConfigurableProgramAnalysis{

  private final CBinaryExpressionBuilder expressionBuilder;
  private final AbstractDomain domain;
  private final ArithmeticOverflowAssumptionBuilder noOverflowAssumptionBuilder;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OverflowCPA.class);
  }

  private OverflowCPA(CFA pCfa, LogManager pLogger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    expressionBuilder = new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
    domain = new FlatLatticeNoTopDomain();
    noOverflowAssumptionBuilder =
        new ArithmeticOverflowAssumptionBuilder(pCfa, pLogger, pConfiguration);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state,
      Precision precision,
      CFAEdge cfaEdge
  ) throws CPATransferException, InterruptedException {
    OverflowState prev = (OverflowState) state;

    if (prev.hasOverflow()) {

      // Once we have an overflow there is no need to continue.
      return Collections.emptyList();
    }


    List<CExpression> assumptions = noOverflowAssumptionBuilder.assumptionsForEdge(cfaEdge);
    if (assumptions.isEmpty()) {
      return ImmutableList.of(new OverflowState(ImmutableList.of(), false));
    }

    // No overflows <=> all assumptions hold.
    List<? extends AExpression> noOverflows;
    if (assumptions.isEmpty()) {
      noOverflows = Collections.emptyList();
    } else {
      noOverflows = assumptions;
    }

    ImmutableList.Builder<OverflowState> outStates = ImmutableList.builder();
    outStates.addAll(
        Lists.transform(
            assumptions,
            // Overflow <=> there exists a violating assumption.
            a -> new OverflowState(ImmutableList.of(mkNot(a)), true)));
    outStates.add(new OverflowState(noOverflows, false));
    return outStates.build();
  }

  private CExpression mkNot(CExpression arg) {
    try {
      return expressionBuilder.negateExpressionAndSimplify(arg);
    } catch (UnrecognizedCCodeException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(domain);
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new OverflowState(ImmutableList.of(), false);
  }
}
