/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockState;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.lock.LockPrecision;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class LockRefiner
    extends
    WrappedConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final LockTransferRelation transfer;
  private final Set<CFAEdge> controlPrecision;
  private final Set<CFAEdge> currentIterationPrecision;
  private AbstractLockState initialLockState;

  // Statistics
  private StatTimer simplifyPath = new StatTimer("Time for path simplification");
  private StatTimer fullStateTimer = new StatTimer("Time for calculating real state");
  private StatTimer newPrecisionTimer = new StatTimer("Time for calculating a new precision");
  private StatCounter numberOfTrueResults = new StatCounter("Number of true results");
  private StatCounter numberOfFalseResults = new StatCounter("Number of false results");
  // private StatCounter numberOfRepeatedConstructedPaths = new StatCounter("Number of repeated path
  // computed");

  public LockRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      LockTransferRelation pTransfer) {
    super(pWrapper);
    transfer = pTransfer;
    controlPrecision = new HashSet<>();
    currentIterationPrecision = new HashSet<>();
  }

  protected AbstractLockState findLastState(List<CFAEdge> edges)
      throws CPAException {
    AbstractLockState currentState = initialLockState;

    for (CFAEdge edge : edges) {
      Collection<? extends AbstractState> successors =
          transfer
              .getAbstractSuccessorsForEdge(currentState, SingletonPrecision.getInstance(), edge);

      if (successors.size() == 0) {
        // Means we do not consider assume edge
        return null;
      }
      currentState = (AbstractLockState) Iterables.getOnlyElement(successors);
    }

    return currentState;
  }

  @Override
  public RefinementResult performBlockRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput)
      throws CPAException, InterruptedException {

    ExtendedARGPath firstPath = pInput.getFirst();
    ExtendedARGPath secondPath = pInput.getSecond();
    simplifyPath.start();
    List<CFAEdge> firstEdges = filterEdges(firstPath);
    List<CFAEdge> secondEdges = filterEdges(secondPath);
    simplifyPath.stop();
    // We cannot use last arg state, as it is not expanded!
    // LockState in Usage is correct
    ARGState initialState = firstPath.getFirstState();
    initialLockState =
        AbstractStates.extractStateByType(initialState, AbstractLockState.class);
    AbstractLockState firstLastLockState = firstPath.getUsageInfo().getLockState();
    AbstractLockState secondLastLockState = secondPath.getUsageInfo().getLockState();

    fullStateTimer.start();
    AbstractLockState firstRealState = findLastState(firstEdges);
    AbstractLockState secondRealState = findLastState(secondEdges);
    fullStateTimer.stop();

    List<Pair<CFANode, LockIdentifier>> toPrecision = new ArrayList<>();
    List<ARGState> firstPairs;
    List<ARGState> secondPairs;

    if (firstRealState == null) {
      // The path is infeasible due to missed lock assumption
      List<Pair<CFANode, LockIdentifier>> prec = getNewPrecision(null, firstEdges);
      toPrecision.addAll(prec);
      firstPairs =
          getAffectedStates(
              from(prec).transform(p -> p.getFirst()).toList(),
              firstPath.asStatesList());
      firstPath.setAsFalse();
      secondPairs = Collections.emptyList();

    } else if (secondRealState == null) {
      List<Pair<CFANode, LockIdentifier>> prec = getNewPrecision(null, secondEdges);
      toPrecision.addAll(prec);
      secondPairs =
          getAffectedStates(
              from(prec).transform(p -> p.getFirst()).toList(),
              secondPath.asStatesList());
      secondPath.setAsFalse();
      firstPairs = Collections.emptyList();

    } else {

      if (firstRealState.isCompatibleWith(secondRealState)) {
        numberOfTrueResults.inc();
        return RefinementResult.createTrue(firstPath, secondPath);
      }

      Collection<LockIdentifier> ids = firstRealState.getIntersection(secondRealState);
      assert !ids.isEmpty();
      // assert firstLastLockState.getCounter(id) == 0 || secondLastLockState.getCounter(id) == 0;
      LockIdentifier id = Iterables.getLast(ids);

      if (firstLastLockState.getCounter(id) == 0) {
        List<Pair<CFANode, LockIdentifier>> prec = getNewPrecision(id, firstEdges);
        toPrecision.addAll(prec);
        firstPairs =
            getAffectedStates(
                from(prec).transform(p -> p.getFirst()).toList(),
                firstPath.asStatesList());
        firstPath.setAsFalse();
      } else {
        firstPairs = Collections.emptyList();
      }
      if (secondLastLockState.getCounter(id) == 0) {
        List<Pair<CFANode, LockIdentifier>> prec = getNewPrecision(id, secondEdges);
        toPrecision.addAll(prec);
        secondPairs =
            getAffectedStates(
                from(prec).transform(p -> p.getFirst()).toList(),
                secondPath.asStatesList());
        secondPath.setAsFalse();
        if (secondPairs.equals(firstPairs)) {
          secondPairs = Collections.emptyList();
        }
      } else {
        secondPairs = Collections.emptyList();
      }
    }

    RefinementResult result = RefinementResult.createFalse();
    result.addPrecision(new LockPrecision(toPrecision));
    result.addInfo(LockRefiner.class, Pair.of(firstPairs, secondPairs));
    numberOfFalseResults.inc();
    return result;
  }

  private List<Pair<CFANode, LockIdentifier>>
      getNewPrecision(
          LockIdentifier pId,
          List<CFAEdge> pEdges)
          throws UnrecognizedCodeException {
    AbstractLockState currentState = initialLockState;

    newPrecisionTimer.start();
    // TODO may be filtered by lock, if we filter only lock effects
    // Save effects are not related to a particular lock
    List<CFAEdge> relatedEdges = pEdges;
    // from(pEdges).filter(e -> transfer.getAffectedLocks(e).contains(pId)).toList();

    List<CFAEdge> filteredEdges = new ArrayList<>();

    for (CFAEdge edge : relatedEdges) {
      Collection<? extends AbstractState> successors =
          transfer
              .getAbstractSuccessorsForEdge(currentState, SingletonPrecision.getInstance(), edge);

      if (successors.isEmpty() && pId == null) {
        pId = Iterables.getOnlyElement(transfer.getAffectedLocks(edge));
        break;
      }

      currentState = (AbstractLockState) Iterables.getOnlyElement(successors);
      if (currentState.compareTo(initialLockState) == 0) {
        filteredEdges.clear();
      } else {
        filteredEdges.add(edge);
      }
    }
    assert !filteredEdges.isEmpty();
    // assert !from(filteredEdges).anyMatch(controlPrecision::contains) : "edge was already added";

    // Do not add it directly, as it may be obtained in other iterations
    currentIterationPrecision.addAll(filteredEdges);

    final LockIdentifier fId = pId;
    List<Pair<CFANode, LockIdentifier>> set =
        from(filteredEdges).transform(e -> Pair.of(e.getPredecessor(), fId)).toList();
    newPrecisionTimer.stop();
    return set;
  }

  private List<CFAEdge> filterEdges(ExtendedARGPath pPath) {
    return from(pPath.getInnerEdges())
        .filter(e -> !(e instanceof CDeclarationEdge || e instanceof BlankEdge))
        .toList();
  }

  private List<ARGState> getAffectedStates(List<CFANode> pNodes, List<ARGState> states) {
    return from(states).filter(s -> pNodes.contains(AbstractStates.extractLocation(s))).toList();
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    pOut.spacer()
        .put(simplifyPath)
        .put(fullStateTimer)
        .put(newPrecisionTimer)
        .put(numberOfTrueResults)
        .put(numberOfFalseResults);

  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      controlPrecision.addAll(currentIterationPrecision);
      currentIterationPrecision.clear();
    }
  }
}
