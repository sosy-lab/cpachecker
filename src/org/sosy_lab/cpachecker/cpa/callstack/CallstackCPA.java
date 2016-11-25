/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.callstack;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import com.google.common.collect.Iterables;

public class CallstackCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA(Configuration config, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super("sep", "sep",
        new DomainInitializer(config).initializeDomain(),
        new TransferInitializer(config).initializeTransfer(config, pLogger));
    this.cfa = pCFA;
  }

  @Override
  public Reducer getReducer() {
    return new CallstackReducer();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().get();
      Collection<Loop> artificialLoops = loopStructure.getLoopsForFunction(
          CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);

      if (!artificialLoops.isEmpty()) {
        Loop singleLoop = Iterables.getOnlyElement(artificialLoops);
        if (singleLoop.getLoopNodes().contains(pNode)) {
          return new CallstackState(
              null,
              CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME,
              pNode
          );
        }
      }
    }
    return new CallstackState(null, pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> computedSuccessors =
        getTransferRelation().getAbstractSuccessorsForEdge(
            pElement, SingletonPrecision.getInstance(), pCfaEdge);
    if (!(pSuccessors instanceof Set) || !(computedSuccessors instanceof Set)
        || pSuccessors.size() != computedSuccessors.size()) { return false; }
    boolean found;
    for (AbstractState e1 : pSuccessors) {
      found = false;
      for (AbstractState e2 : computedSuccessors) {
        if (((CallstackState) e1).sameStateInProofChecking((CallstackState) e2)) {
          found = true;
          break;
        }
      }
      if (!found) { return false; }
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement) throws CPAException, InterruptedException {
    return (getAbstractDomain().isLessOrEqual(pElement, pOtherElement)) || ((CallstackState) pElement)
        .sameStateInProofChecking((CallstackState) pOtherElement);
  }

  @Override
  public boolean isCoveredByRecursiveState(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return new CallstackStateEqualsWrapper((CallstackState) state1)
        .equals(new CallstackStateEqualsWrapper((CallstackState) state2));
  }

  @Options(prefix = "cpa.callstack")
  private static class DomainInitializer {

    @Option(secure = true, name = "domain", toUppercase = true, values = { "FLAT", "FLATPCC" },
        description = "which abstract domain to use for callstack cpa, typically FLAT which is faster since it uses only object equivalence")
    private String domainType = "FLAT";

    public DomainInitializer(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public AbstractDomain initializeDomain() throws InvalidConfigurationException {
      switch (domainType) {
      case "FLAT":
        return new FlatLatticeDomain();
      case "FLATPCC":
        return new CallstackPCCAbstractDomain();
      default:
        throw new InvalidConfigurationException("Unknown domain type for callstack cpa.");
      }
    }
  }

  @Options(prefix = "cpa.callstack")
  private static class TransferInitializer {

    @Option(description="analyse the CFA backwards", secure=true)
    private boolean traverseBackwards = false;

    public TransferInitializer(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public TransferRelation initializeTransfer(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
      if (traverseBackwards) {
        return new CallstackTransferRelationBackwards(pConfig, pLogger);
      } else {
        return new CallstackTransferRelation(pConfig, pLogger);
      }
    }
  }

}