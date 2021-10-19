// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.util.StateTransformer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class WorkerAnalysis {

  protected final Solver solver;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bmgr;
  protected final PathFormulaManagerImpl pathFormulaManager;

  protected final Algorithm algorithm;
  protected final ReachedSet reachedSet;

  protected final Precision emptyPrecision;

  protected final BlockNode block;
  protected final LogManager logger;

  protected final ConfigurableProgramAnalysis cpa;

  protected AlgorithmStatus status;

  public WorkerAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(pLogger, pSpecification, pCFA, pConfiguration,
            pShutdownManager,
            ImmutableSet.of(
                "analysis.algorithm.configurableComponents",
                "analysis.useLoopStructure",
                "cpa.predicate.blk.alwaysAtJoin",
                "cpa.predicate.blk.alwaysAtBranch",
                "cpa.predicate.blk.alwaysAtProgramExit"), pBlock);
    algorithm = parts.getFirst();
    cpa = parts.getSecond();
    reachedSet = parts.getThird();

    solver = StateTransformer.extractAnalysis(cpa, PredicateCPA.class).getSolver();
    emptyPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    fmgr = solver.getFormulaManager();
    bmgr = fmgr.getBooleanFormulaManager();
    pathFormulaManager = new PathFormulaManagerImpl(
        solver.getFormulaManager(),
        pConfiguration,
        pLogger,
        pShutdownManager.getNotifier(),
        pCFA,
        AnalysisDirection.FORWARD);
    block = pBlock;
    logger = pLogger;
  }

  protected ARGState getStartState(BooleanFormula condition, CFANode node)
      throws InterruptedException {
    CompositeState compositeState =
        StateTransformer.extractCompositeStateFromAbstractState(getInitialStateFor(node));
    PredicateAbstractState firstPredicateState =
        StateTransformer.getStatesFromCompositeState(compositeState, PredicateAbstractState.class)
            .stream().findFirst()
            .orElseThrow(() -> new AssertionError("Analysis has to contain a PredicateState"));
    firstPredicateState = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(),
            condition),
        firstPredicateState);
    List<AbstractState> states = new ArrayList<>();
    for (AbstractState wrappedState : compositeState.getWrappedStates()) {
      if (wrappedState instanceof PredicateAbstractState) {
        states.add(firstPredicateState);
      } else {
        states.add(wrappedState);
      }
    }
    CompositeState actualStartState = new CompositeState(states);
    return new ARGState(actualStartState, null);
  }

  private AbstractState getInitialStateFor(CFANode pNode) throws InterruptedException {
    return cpa.getInitialState(pNode, StateSpacePartition.getDefaultPartition());
  }

  public Solver getSolver() {
    return solver;
  }

  public FormulaManagerView getFmgr() {
    return fmgr;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public BooleanFormulaManagerView getBmgr() {
    return bmgr;
  }

  public PathFormulaManagerImpl getPathFormulaManager() {
    return pathFormulaManager;
  }

  public Precision getEmptyPrecision() {
    return emptyPrecision;
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  public abstract Message analyze(BooleanFormula condition, CFANode node)
      throws CPAException, InterruptedException;

  public static class ForwardAnalysis extends WorkerAnalysis {

    public ForwardAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(pLogger, pBlock, pCFA, pSpecification, pConfiguration, pShutdownManager);
    }

    @Override
    public Message analyze(BooleanFormula condition, CFANode node)
        throws CPAException, InterruptedException {
      reachedSet.clear();
      reachedSet.add(getStartState(condition, node), emptyPrecision);
      status = algorithm.run(reachedSet);
      Optional<ARGState> targetState = from(reachedSet).filter(AbstractStates::isTargetState)
          .filter(ARGState.class).first();
      if (targetState.isPresent()) {
        Optional<CFANode> targetNode = StateTransformer.findCFANodeOfState(targetState.get());
        if (!targetNode.isPresent()) {
          throw new AssertionError(
              "States need to have a location but they do not:" + targetState.get());
        }
        return Message.newPostconditionMessage(block.getId(),
            targetNode.get().getNodeNumber(), bmgr.makeTrue(), fmgr);
      }
      Map<AbstractState, BooleanFormula>
          formulas = StateTransformer.transformReachedSet(reachedSet, block.getLastNode(), fmgr);
      BooleanFormula result = formulas.isEmpty() ? bmgr.makeTrue() : bmgr.or(formulas.values());
      return Message.newPreconditionMessage(block.getId(), block.getLastNode().getNodeNumber(), result,
          fmgr);
    }
  }

  public static class BackwardAnalysis extends WorkerAnalysis {

    public BackwardAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(pLogger, pBlock, pCFA, pSpecification, pConfiguration, pShutdownManager);
    }

    @Override
    public Message analyze(BooleanFormula condition, CFANode node) throws CPAException, InterruptedException {
      reachedSet.clear();
      reachedSet.add(getStartState(condition, node), emptyPrecision);
      status = algorithm.run(reachedSet);
      Map<AbstractState, BooleanFormula>
          formulas = StateTransformer.transformReachedSet(reachedSet, block.getStartNode(), fmgr);
      BooleanFormula result = formulas.isEmpty() ? bmgr.makeTrue() : bmgr.or(formulas.values());
      // by definition: if the post-condition reaches the root element, the specification is violated
      if (block.getPredecessors().isEmpty()) {
        return Message.newFinishMessage(block.getId(), block.getStartNode().getNodeNumber(), Result.FALSE);
      }
      return Message.newPostconditionMessage(block.getId(), block.getStartNode().getNodeNumber(), result, fmgr);
    }

    public boolean cantContinue(String currentPreCondition, String receivedPostCondition)
        throws SolverException, InterruptedException {
      return solver.isUnsat(bmgr.and(fmgr.parse(currentPreCondition), fmgr.parse(receivedPostCondition)));
    }
  }
}
