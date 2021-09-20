// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public abstract class WorkerAnalysis {

  protected final Solver solver;
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bmgr;
  protected final PathFormulaManagerImpl pathFormulaManager;

  protected final Algorithm algorithm;
  protected final ReachedSet reachedSet;

  protected final CompositeState startState;
  protected final Precision emptyPrecision;

  protected final BlockNode block;
  protected final LogManager logger;

  protected AlgorithmStatus status;

  public WorkerAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      AnalysisDirection pAnalysisDirection)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(pLogger, pSpecification, pCFA, pConfiguration,
            pShutdownManager,
            ImmutableSet.of(
                "analysis.algorithm.configurableComponents",
                "analysis.useLoopStructure",
                "cpa.predicate.blk.alwaysAtJoin",
                "cpa.predicate.blk.alwaysAtBranch",
                "cpa.predicate.blk.alwaysAtProgramExit"), pBlock, pAnalysisDirection);
    algorithm = parts.getFirst();
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;
    solver = extractAnalysis(parts.getSecond(), PredicateCPA.class).getSolver();
    reachedSet = parts.getThird();
    startState = extractCompositeStateFromReachedSet(reachedSet);
    emptyPrecision = reachedSet.getPrecision(reachedSet.getFirstState());

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

  protected <T extends ConfigurableProgramAnalysis> T extractAnalysis(
      ConfigurableProgramAnalysis cpa,
      Class<T> pTarget) {
    ARGCPA argCpa = (ARGCPA) cpa;
    if (argCpa.getWrappedCPAs().size() > 1) {
      throw new AssertionError("Wrapper expected but got " + cpa + "instead");
    }
    if (!(argCpa.getWrappedCPAs().get(0) instanceof CompositeCPA)) {
      throw new AssertionError(
          "Expected " + CompositeCPA.class + " but got " + argCpa.getWrappedCPAs().get(0)
              .getClass());
    }
    CompositeCPA compositeCPA = (CompositeCPA) argCpa.getWrappedCPAs().get(0);
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      if (wrappedCPA.getClass().equals(pTarget)) {
        return pTarget.cast(wrappedCPA);
      }
    }
    throw new AssertionError(
        "Expected analysis " + pTarget + " is not part of the composite cpa " + cpa);
  }

  protected CompositeState extractCompositeStateFromReachedSet(@Nonnull ReachedSet pReachedSet) {
    AbstractState state = pReachedSet.getFirstState();
    checkNotNull(state, "ReachedSet has to have a starting state");
    checkState(state instanceof ARGState, "First state has to be an ARGState");
    ARGState argState = (ARGState) state;
    checkState(argState.getWrappedState() instanceof CompositeState,
        "First state must contain a CompositeState");
    return (CompositeState) argState.getWrappedState();
  }

  protected <T extends AbstractState> ImmutableSet<T> getStatesFromCompositeState(
      @Nonnull CompositeState pCompositeState,
      Class<T> pTarget) {
    Set<T> result = new HashSet<>();
    for (AbstractState wrappedState : pCompositeState.getWrappedStates()) {
      if (pTarget.isAssignableFrom(wrappedState.getClass())) {
        result.add(pTarget.cast(wrappedState));
      } else if (wrappedState instanceof CompositeState) {
        result.addAll(getStatesFromCompositeState((CompositeState) wrappedState, pTarget));
      }
    }
    return ImmutableSet.copyOf(result);
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

  public CompositeState getStartState() {
    return startState;
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

  public abstract Message analyze(BooleanFormula condition)
      throws CPAException, InterruptedException;


  public static class ForwardAnalysis extends WorkerAnalysis {

    public ForwardAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
      super(pLogger, pBlock, pCFA, pSpecification, pConfiguration, pShutdownManager, AnalysisDirection.FORWARD);
    }

    @Override
    public Message analyze(BooleanFormula condition) throws CPAException, InterruptedException {
      PredicateAbstractState firstPredicateState =
          getStatesFromCompositeState(startState, PredicateAbstractState.class).stream().findFirst()
              .orElseThrow(() -> new AssertionError("Analysis has to contain a PredicateState"));
      firstPredicateState = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
          pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(), fmgr.uninstantiate(condition)),
          firstPredicateState);
      List<AbstractState> states = new ArrayList<>();
      for (AbstractState wrappedState : startState.getWrappedStates()) {
        if (wrappedState instanceof PredicateAbstractState) {
          states.add(firstPredicateState);
        } else {
          states.add(wrappedState);
        }
      }
      CompositeState actualStartState = new CompositeState(states);
      reachedSet.clear();
      reachedSet.add(new ARGState(actualStartState, null), emptyPrecision);
      status = algorithm.run(reachedSet);
      ImmutableSet<ARGState> finalStates = ARGUtils.getFinalStates(reachedSet);
      BooleanFormula preconditionForNextBlock = bmgr.makeFalse();
      if (!finalStates.isEmpty()) {
        for (ARGState l : finalStates) {
          CompositeState compositeState = (CompositeState) l.getWrappedState();
          for (AbstractState wrappedState : compositeState.getWrappedStates()) {
            if (wrappedState instanceof PredicateAbstractState) {
              PredicateAbstractState predicateAbstractState = (PredicateAbstractState) wrappedState;
              preconditionForNextBlock = bmgr.or(preconditionForNextBlock,
                  predicateAbstractState.getPathFormula().getFormula());
              break;
            }
          }
        }
      } else {
        logger.log(Level.WARNING, "The reached set does not contain any states: " + reachedSet);
        preconditionForNextBlock = bmgr.makeTrue();
      }
      return new Message(MessageType.PRECONDITION, block,
          fmgr.dumpArbitraryFormula(preconditionForNextBlock));
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
        throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
      super(pLogger, pBlock, pCFA, pSpecification, pConfiguration, pShutdownManager, AnalysisDirection.BACKWARD);
    }

    @Override
    public Message analyze(BooleanFormula condition) throws CPAException, InterruptedException {
      return new Message(MessageType.POSTCONDITION, block,
          fmgr.dumpArbitraryFormula(bmgr.makeTrue()));
    }
  }
}
