// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.value.SummaryEdge;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSummaryCache;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/**
 * This is an extension of {@link AbstractARGBasedRefiner} that takes care of flattening the ARG
 * before calling {@link ARGBasedRefiner#performRefinementForPath(ARGReachedSet, ARGPath)}.
 *
 * <p>Warning: Although the ARG is flattened at this point, the elements in it have not been
 * expanded due to performance reasons.
 */
public final class BAMBasedRefinerWithSummaryReuse extends AbstractARGBasedRefiner {

  private final BAMBasedRefiner BamBasedRefiner;
  private final StrongestPostOperator<ValueAnalysisState> strongestPostOp;
  private final VariableTrackingPrecision precision;
  private final ValueAnalysisState initialState;

  private BAMBasedRefinerWithSummaryReuse(
      BAMBasedRefiner pBAMBasedRefiner,
      ARGBasedRefiner pRefiner,
      ARGCPA pArgCpa,
      LogManager pLogger,
      VariableTrackingPrecision pPrecision,
      final CFA pCfa,
      final Configuration config)
      throws InvalidConfigurationException {
    super(pRefiner, pArgCpa, pLogger);

    strongestPostOp = new ValueAnalysisStrongestPostOperator(pLogger, config, pCfa);

    initialState = new ValueAnalysisState(pCfa.getMachineModel());
    BamBasedRefiner = pBAMBasedRefiner;
    precision = pPrecision;
  }

  /**
   * Create a {@link Refiner} instance that supports BAM from a {@link ARGBasedRefiner} instance.
   */
  public static Refiner forARGBasedRefiner(
      final ARGBasedRefiner pRefiner, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    checkArgument(
        !(pRefiner instanceof Refiner),
        "ARGBasedRefiners may not implement Refiner, choose between these two!");

    if (!(pCpa instanceof AbstractBAMCPA bamCpa)) {
      throw new InvalidConfigurationException("BAM CPA needed for BAM-based refinement");
    }

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);

    var bamBasedRefiner = (BAMBasedRefiner) BAMBasedRefiner.forARGBasedRefiner(pRefiner, pCpa);

    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, BAMBasedRefinerWithSummaryReuse.class);

    var logger = bamCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    var precision =
        VariableTrackingPrecision.createStaticPrecision(
            config, cfa.getVarClassification(), ValueAnalysisCPA.class);

    return new BAMBasedRefinerWithSummaryReuse(
        bamBasedRefiner, pRefiner, argCpa, logger, precision, cfa, config);
  }

  @Override
  protected CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    return BamBasedRefiner.performRefinementForPath(pReached, pPath);
  }

  @Override
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pMainReachedSet)
      throws InterruptedException, CPATransferException {
    var path = BamBasedRefiner.computePath(pLastElement, pMainReachedSet);

    try {
      boolean isInfeasible = isInfeasible(path);
      int firstHoleIndex = getFirstHoleIndex(path);

      while (firstHoleIndex != -1 && !isInfeasible) {
        path = expandHole(path, firstHoleIndex);
        isInfeasible = isInfeasible(path);
        firstHoleIndex = getFirstHoleIndex(path);
      }
    } catch (CPAException e) {
      throw new CPATransferException(e.getMessage(), e);
    }

    return path;
  }

  private boolean isInfeasible(ARGPath pPath) throws CPAException, InterruptedException {
    ValueAnalysisState next = initialState;
    var callStack = new ArrayDeque<ValueAnalysisState>();

    PathIterator iterator = pPath.fullPathIterator();
    while (iterator.hasNext()) {
      final CFAEdge edge = iterator.getOutgoingEdge();

      Optional<ValueAnalysisState> maybeNext;
      if (!(edge instanceof SummaryEdge)) {
        maybeNext = strongestPostOp.step(next, edge, precision, callStack, pPath);
      } else {
        var state = iterator.getAbstractState();
        var node = AbstractStates.extractLocation(state);
        var valueState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);
        var summary = ValueAnalysisSummaryCache.getInstance().getApplicableSummary(node, valueState);
        if (summary != null) {
          maybeNext = Optional.of(summary.applyToState(next));
        } else {
          maybeNext = Optional.empty();
        }
      }

      if (maybeNext.isEmpty()) {
        return true;
      } else {
        next = maybeNext.get();
      }

      iterator.advance();
    }

    return false;
  }

  private ARGPath expandHole(ARGPath pPath, int holeIndex)
      throws CPAException, InterruptedException {
    var holeStart = pPath.asStatesList().get(holeIndex);
    var holeEnd = pPath.asStatesList().get(holeIndex + 1);

    var edge = pPath.getInnerEdges().get(holeIndex);
    var currentNode = edge.getPredecessor();
    var returnNode = edge.getSuccessor();

    var callStack = new ArrayDeque<ValueAnalysisState>();

    var calls = new ArrayDeque<CFANode>();

    var waitList = new ArrayDeque<BackwardARGState>();
    edge = currentNode.getLeavingEdge(0);
    waitList.add(expandEdge((BackwardARGState) holeStart, edge, callStack, pPath).get());

    while (!waitList.isEmpty()) {
      var argState = waitList.remove();
      currentNode = AbstractStates.extractLocation(argState);

      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        edge = currentNode.getLeavingEdge(i);

        if (edge instanceof FunctionCallEdge callEdge) {
          var ExitNode = callEdge.getSummaryEdge().getSuccessor();
          calls.push(ExitNode);
        }

        if (edge instanceof FunctionReturnEdge) {
          if (!edge.getSuccessor().equals(calls.peek())) {
            continue;
          } else {
            calls.pop();
          }
        }

        var maybeNext = expandEdge(argState, edge, callStack, pPath);

        if (maybeNext.isPresent()) {
          var suc = edge.getSuccessor();

          if (suc.equals(returnNode)) {
            holeEnd.removeParent(holeStart);
            holeEnd.addParent(argState);
            return ARGUtils.getOnePathTo(pPath.getLastState());
          } else {
            waitList.add(maybeNext.get());
          }
        }
      }
    }

    return pPath;
  }

  private Optional<BackwardARGState> expandEdge(
      BackwardARGState state, CFAEdge edge, Deque<ValueAnalysisState> callstack, ARGPath path)
      throws CPAException, InterruptedException {
    ValueAnalysisState currentState =
        AbstractStates.extractStateByType(state, ValueAnalysisState.class);

    var maybeNext = strongestPostOp.step(currentState, edge, precision, callstack, path);

    if (maybeNext.isPresent()) {
      var suc = edge.getSuccessor();

      var locationState =
          GlobalInfo.getInstance().getCFAInfo().get().getLocationStateFactory().getState(suc);
      var valueState = maybeNext.get();
      var states = new ArrayList<AbstractState>();
      states.add(locationState);
      states.add(valueState);
      var newArgState =
          new BackwardARGState(new ARGState(new CompositeState(states), state.getARGState()));
      newArgState.addParent(state);
      return Optional.of(newArgState);
    }
    return Optional.empty();
  }

  private int getFirstHoleIndex(ARGPath pPath) {
    var edges = pPath.getInnerEdges();
    for (int i = 0; i < edges.size(); i++) {
      var edge = edges.get(i);
      if (edge instanceof SummaryEdge) {
        return i;
      }
    }
    return -1;
  }
}

