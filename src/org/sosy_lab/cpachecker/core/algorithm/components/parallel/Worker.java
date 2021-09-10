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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Worker implements Runnable {

  private final BlockNode block;
  private final BlockingQueue<Message> read;
  private final BlockingQueue<Message> write;

  private final ConcurrentHashMap<BlockNode, Message> postConditionUpdates;
  private final ConcurrentHashMap<BlockNode, Message> preConditionUpdates;
  private final BooleanFormulaManagerView bmgr;
  private final PathFormulaManagerImpl pathFormulaManager;

  private final LogManager logger;

  private final Algorithm algorithm;
  private final ReachedSet reachedSet;

  private boolean finished;
  private AlgorithmStatus status;

  private final CompositeState startState;

  public Worker(
      BlockNode pBlock,
      BlockingQueue<Message> pOutputStream,
      BlockingQueue<Message> pInputStream,
      LogManager pLogger,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, IOException, InterruptedException, InvalidConfigurationException {
    block = pBlock;
    read = pOutputStream;
    write = pInputStream;
    postConditionUpdates = new ConcurrentHashMap<>();
    preConditionUpdates = new ConcurrentHashMap<>();
    Solver solver = Solver.create(pConfiguration, pLogger, pShutdownManager.getNotifier());
    pathFormulaManager = new PathFormulaManagerImpl(
        solver.getFormulaManager(),
        pConfiguration,
        pLogger,
        pShutdownManager.getNotifier(),
        pCFA,
        AnalysisDirection.FORWARD);
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    logger = pLogger;
    finished = false;
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet>  parts = new AlgorithmBuilder(logger, pSpecification, pCFA, pConfiguration).createAlgorithm(pShutdownManager,
        ImmutableSet.of(
            "analysis.algorithm.configurableComponents",
            "analysis.useLoopStructure",
            "cpa.predicate.blk.alwaysAtJoin",
            "cpa.predicate.blk.alwaysAtBranch",
            "cpa.predicate.blk.alwaysAtProgramExit"), ImmutableSet.of(), pBlock);
    checkNotNull(parts.getFirst());
    checkNotNull(parts.getSecond());
    checkNotNull(parts.getThird());
    algorithm = parts.getFirst();
    reachedSet = parts.getThird();
    startState = extractCompositeStateFromReachedSet(reachedSet);

    postConditionUpdates.put(block, new Message(MessageType.PRECONDITION, block, bmgr.makeTrue(), reachedSet.getPrecision(reachedSet.getFirstState())));
    preConditionUpdates.put(block, new Message(MessageType.POSTCONDITION, block, bmgr.makeTrue(), reachedSet.getPrecision(reachedSet.getFirstState())));
  }

  private CompositeState extractCompositeStateFromReachedSet(@Nonnull ReachedSet pReachedSet) {
    AbstractState state = pReachedSet.getFirstState();
    checkNotNull(state, "ReachedSet has to have a starting state");
    checkState(state instanceof ARGState, "First state has to be an ARGState");
    ARGState argState = (ARGState) state;
    checkState(argState.getWrappedState() instanceof CompositeState, "First state must contain a CompositeState");
    return (CompositeState) argState.getWrappedState();
  }

  private <T> ImmutableSet<T> getStatesFromCompositeState(Class<T> pStateClass, @Nonnull CompositeState pCompositeState) {
    Set<T> result = new HashSet<>();
    for (AbstractState wrappedState : pCompositeState.getWrappedStates()) {
      if (wrappedState.getClass().equals(pStateClass) || pStateClass.isAssignableFrom(wrappedState.getClass())) {
        result.add((T) wrappedState);
      }
    }
    return ImmutableSet.copyOf(result);
  }

  public BooleanFormula getPostCondition() {
    checkState(preConditionUpdates.containsKey(block), "preConditionUpdates must contain own pre-condition");
    return postConditionUpdates.get(block).getCondition();
  }

  public BooleanFormula getPreCondition() {
    checkState(preConditionUpdates.containsKey(block), "preConditionUpdates must contain own pre-condition");
    return preConditionUpdates.get(block).getCondition();
  }

  public void analyze() throws InterruptedException, CPAException {
    while (true) {
      Message m = read.take();
      processMessage(m);
      if (finished) {
        return;
      }
    }
  }

  private void processMessage(Message message) throws InterruptedException, CPAException {
    switch (message.getType()) {
      case FINISHED:
        finished = true;
        return;
      case PRECONDITION:
        if (message.getSender().getSuccessors().contains(block)) {
          preConditionUpdates.put(message.getSender(), message);
          write.add(forwardAnalysis());
        }
        break;
      case POSTCONDITION:
        if (message.getSender().getPredecessors().contains(block)) {
          postConditionUpdates.put(message.getSender(), message);
          write.add(backwardAnalysis());
        }
        break;
      default:
        throw new AssertionError("Message type " + message.getType() + " does not exist");
    }
  }

  private Precision getPreconditionPrecision() {
    checkState(preConditionUpdates.containsKey(block), "preConditionUpdates must contain own pre-condition");
    return preConditionUpdates.get(block).getPrecision();
  }

  // return post condition
  private Message forwardAnalysis() throws CPAException, InterruptedException {
    reachedSet.clear();
    PredicateAbstractState firstPredicateState =
        getStatesFromCompositeState(PredicateAbstractState.class, startState).stream().findFirst()
            .orElseThrow(() -> new AssertionError("Analysis has to contain a PredicateState"));
    firstPredicateState = PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
        pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(), getPreCondition()),
        firstPredicateState);
    List<AbstractState> states = new ArrayList<>();
    for (AbstractState wrappedState : startState.getWrappedStates()) {
      if (!(wrappedState instanceof PredicateAbstractState)) {
        states.add(wrappedState);
      } else {
        states.add(firstPredicateState);
      }
    }
    CompositeState actualStartState = new CompositeState(states);
    reachedSet.clear();
    reachedSet.add(new ARGState(actualStartState, null), getPreconditionPrecision());
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
    return new Message(MessageType.PRECONDITION, block, preconditionForNextBlock,
        reachedSet.getPrecision(reachedSet.getLastState()));
  }

  // return pre condition
  private Message backwardAnalysis() {
    return new Message(MessageType.POSTCONDITION, block, bmgr.makeTrue(), reachedSet.getPrecision(reachedSet.getLastState()));
  }

  private void runContinuousAnalysis() {
    try {
      analyze();
    } catch (InterruptedException | CPAException pE) {
      if (!finished) {
        logger.log(Level.SEVERE, this + " run into an error while waiting because of " + pE);
        logger.log(Level.SEVERE, "Restarting Worker " + this + "...");
        runContinuousAnalysis();
      } else {
        logger.log(
            Level.SEVERE,
            this
                + " run into an error while waiting because of "
                + pE
                + " but there is nothing to do because analysis finished before.");
      }
    }
  }

  @Override
  public void run() {
    try {
      write.add(forwardAnalysis());
      runContinuousAnalysis();
    } catch (CPAException | InterruptedException pE) {
      run();
    }
  }

  @Override
  public String toString() {
    return "Worker{" + "block=" + block + ", finished=" + finished + '}';
  }

  public AlgorithmStatus getStatus() {
    return status;
  }
}
