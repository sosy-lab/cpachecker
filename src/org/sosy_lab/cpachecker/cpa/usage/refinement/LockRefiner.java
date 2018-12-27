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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.lock.AbstractLockState;
import org.sosy_lab.cpachecker.cpa.lock.LockIdentifier;
import org.sosy_lab.cpachecker.cpa.lock.LockPrecision;
import org.sosy_lab.cpachecker.cpa.lock.LockReducer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.lock.effects.AbstractLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
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
  private final LockReducer reducer;
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
      LockTransferRelation pTransfer,
      LockReducer pReducer) {
    super(pWrapper);
    transfer = pTransfer;
    reducer = pReducer;
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

    Iterable<Entry<CFANode, LockIdentifier>> toPrecision = Collections.emptySet();
    List<ARGState> firstPairs;
    List<ARGState> secondPairs;

    if (firstRealState == null) {
      // The path is infeasible due to missed lock assumption
      Iterable<Entry<CFANode, LockIdentifier>> prec = getNewPrecision(null, firstEdges);
      toPrecision = Iterables.concat(toPrecision, prec);
      firstPairs =
          getAffectedStates(prec, firstPath);
      secondPairs = Collections.emptyList();

    } else if (secondRealState == null) {
      Iterable<Entry<CFANode, LockIdentifier>> prec = getNewPrecision(null, secondEdges);
      toPrecision = Iterables.concat(toPrecision, prec);
      secondPairs =
          getAffectedStates(prec, secondPath);
      firstPairs = Collections.emptyList();

    } else {

      if (firstRealState.isCompatibleWith(secondRealState)) {
        numberOfTrueResults.inc();
        return wrappedRefiner.performBlockRefinement(pInput);
      }

      Collection<LockIdentifier> ids = firstRealState.getIntersection(secondRealState);
      assert !ids.isEmpty();
      // assert firstLastLockState.getCounter(id) == 0 || secondLastLockState.getCounter(id) == 0;
      LockIdentifier id = Iterables.getLast(ids);

      if (firstLastLockState.getCounter(id) == 0) {
        Iterable<Entry<CFANode, LockIdentifier>> prec = getNewPrecision(id, firstEdges);
        toPrecision = Iterables.concat(toPrecision, prec);
        firstPairs =
            getAffectedStates(prec, firstPath);
      } else {
        firstPairs = Collections.emptyList();
      }
      if (secondLastLockState.getCounter(id) == 0) {
        Iterable<Entry<CFANode, LockIdentifier>> prec = getNewPrecision(id, secondEdges);
        toPrecision = Iterables.concat(toPrecision, prec);
        secondPairs =
            getAffectedStates(prec, secondPath);
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

  private Iterable<Entry<CFANode, LockIdentifier>>
      getNewPrecision(
          LockIdentifier pId,
          List<CFAEdge> pEdges)
          throws UnrecognizedCodeException {
    AbstractLockState currentState = initialLockState;

    newPrecisionTimer.start();

    List<CFAEdge> filteredEdges = new ArrayList<>();
    boolean inAnnotatedContext = false;

    for (CFAEdge edge : pEdges) {
      List<AbstractLockEffect> effects = transfer.determineOperations(edge);

      if (pId != null) {
        boolean valuable = false;
        for (AbstractLockEffect e : effects) {
          if (e instanceof LockEffect) {
            if (((LockEffect) e).getAffectedLock().equals(pId)) {
              valuable = true;
              break;
            }
          } else {
            // Save effects are not related to a particular lock
            valuable = true;
            inAnnotatedContext = true;
            break;
          }
        }
        if (!valuable) {
          continue;
        }
      }

      currentState = transfer.applyEffects(currentState, effects);

      if (currentState == null) {
        assert pId == null;
        pId = Iterables.getOnlyElement(transfer.getAffectedLocks(edge));
        break;
      }

      if (currentState.compareTo(initialLockState) == 0 && !inAnnotatedContext) {
        filteredEdges.clear();
      } else {
        filteredEdges.add(edge);
      }
    }
    assert !filteredEdges.isEmpty();
    // assert !from(filteredEdges).anyMatch(controlPrecision::contains) : "edge was already added";

    final LockIdentifier fId = pId;
    List<FunctionEntryNode> stack = getStack(pEdges);
    reducer.consider(stack, fId);

    Map<CFANode, LockIdentifier> set =
        from(filteredEdges).transform(e -> e.getPredecessor()).toMap(e -> fId);
    newPrecisionTimer.stop();
    return set.entrySet();
  }

  private List<CFAEdge> filterEdges(ExtendedARGPath pPath) {
    return from(pPath.getInnerEdges())
        .filter(e -> !(e instanceof CDeclarationEdge || e instanceof BlankEdge))
        .toList();
  }

  private List<ARGState>
      getAffectedStates(Iterable<Entry<CFANode, LockIdentifier>> prec, ExtendedARGPath pPath) {
    FluentIterable<CFANode> nodes = from(prec).transform(p -> p.getKey());
    pPath.setAsFalse();
    return from(pPath.asStatesList())
        .filter(s -> nodes.contains(AbstractStates.extractLocation(s)))
        .toList();
  }

  private List<FunctionEntryNode> getStack(List<CFAEdge> edges) {

    List<FunctionEntryNode> entryNodes = new ArrayList<>();

    for (CFAEdge edge : edges) {
      if (edge instanceof CFunctionCallEdge) {
        entryNodes.add(((CFunctionCallEdge) edge).getSuccessor());
      } else if (edge instanceof FunctionReturnEdge) {
        entryNodes.remove(entryNodes.size() - 1);
      } else {
        // skip any other edge
      }
    }
    return entryNodes;
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    pOut.spacer()
        .put(simplifyPath)
        .put(fullStateTimer)
        .put(newPrecisionTimer)
        .put(numberOfTrueResults)
        .put(numberOfFalseResults);

    wrappedRefiner.printStatistics(pOut);
  }
}
