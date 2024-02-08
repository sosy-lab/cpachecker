// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class CallstackCPA extends AbstractCPA
    implements ConfigurableProgramAnalysisWithBAM, ProofChecker {

  private final CallstackOptions options;
  private final LogManager logger;

  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(CallstackCPA.class);
  }

  public CallstackCPA(Configuration config, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    logger = pLogger;
    options = new CallstackOptions(config);
    this.cfa = pCFA;
  }

  @Override
  public Reducer getReducer() {
    return new CallstackReducer();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().orElseThrow();
      Collection<Loop> artificialLoops =
          loopStructure.getLoopsForFunction(
              CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);

      if (!artificialLoops.isEmpty()) {
        Loop singleLoop = Iterables.getOnlyElement(artificialLoops);
        if (singleLoop.getLoopNodes().contains(pNode)) {
          return new CallstackState(
              null, CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME, pNode);
        }
      }
    }
    return new CallstackState(null, pNode.getFunctionName(), pNode);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> computedSuccessors =
        getTransferRelation()
            .getAbstractSuccessorsForEdge(pElement, SingletonPrecision.getInstance(), pCfaEdge);
    if (!(pSuccessors instanceof Set)
        || !(computedSuccessors instanceof Set)
        || pSuccessors.size() != computedSuccessors.size()) {
      return false;
    }
    boolean found;
    for (AbstractState e1 : pSuccessors) {
      found = false;
      for (AbstractState e2 : computedSuccessors) {
        if (((CallstackState) e1).sameStateInProofChecking((CallstackState) e2)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pElement, AbstractState pOtherElement)
      throws CPAException, InterruptedException {
    return getAbstractDomain().isLessOrEqual(pElement, pOtherElement)
        || ((CallstackState) pElement).sameStateInProofChecking((CallstackState) pOtherElement);
  }

  @Override
  public boolean isCoveredByRecursiveState(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return new CallstackStateEqualsWrapper((CallstackState) state1)
        .equals(new CallstackStateEqualsWrapper((CallstackState) state2));
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return switch (options.getDomainType()) {
      case "FLAT" -> new FlatLatticeDomain();
      case "FLATPCC" -> new CallstackPCCAbstractDomain();
      default -> // InvalidCongifurationException already thrown by ConfigurationOption due to
          // values-field
          throw new AssertionError("Unknown domain type for callstack cpa.");
    };
  }

  @Override
  public CallstackTransferRelation getTransferRelation() {
    if (options.traverseBackwards()) {
      return new CallstackTransferRelationBackwards(options, logger);
    } else {
      return new CallstackTransferRelation(options, logger);
    }
  }
}
