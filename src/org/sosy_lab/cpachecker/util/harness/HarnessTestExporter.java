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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.harness.HarnessState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter.State;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter.TargetTestVector;

@Options(prefix = "testHarnessTestExport")
public class HarnessTestExporter {

  private final CFA cfa;

  private final LogManager logger;

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  @Option(secure = true, description = "Use the counterexample model to provide test-vector values")
  private boolean useModel = true;

  public HarnessTestExporter(Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    cfa = pCfa;
    logger = pLogger;
    pConfig.inject(this);
  }

  public void writeTest(
      Appendable pAppendable,
      ARGState pRootState,
      Predicate<ARGState> pIsRelevantState,
      Predicate<Pair<ARGState, ARGState>> pIsRelevantEdge,
      CounterexampleInfo pCounterexampleInfo) {

    Optional<TargetTestVector> testVector =
        extractTestVector(
            pRootState,
            pIsRelevantState,
            pIsRelevantEdge,
            getValueMap(pCounterexampleInfo));

    TestAppender testAppender = new TestAppender(pAppendable);

    testAppender.appendTest(testVector);

    return;

  }

  private Optional<TargetTestVector> extractTestVector(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    Set<State> visited = Sets.newHashSet();
    Deque<State> stack = Queues.newArrayDeque();
    Deque<CFAEdge> lastEdgeStack = Queues.newArrayDeque();
    stack.push(State.of(pRootState, TestVector.newTestVector()));
    visited.addAll(stack);
    while (!stack.isEmpty()) {
      State previous = stack.pop();
      CFAEdge lastEdge = null;
      if (!lastEdgeStack.isEmpty()) {
        lastEdge = lastEdgeStack.pop();
      }
      if (AbstractStates.isTargetState(previous.argState)) {
        assert lastEdge != null : "Expected target state to be different from root state, but was not";
        TestVector testVectorWithPointerIndices =
            addIndicesFromCPA(previous.testVector, previous.argState);
        return Optional.of(new TargetTestVector(lastEdge, testVectorWithPointerIndices));
      }
      ARGState parent = previous.argState;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.apply(Pair.of(parent, child))) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              if (parentLoc.hasEdgeTo(childLoc)) {
                CFAEdge edge = parentLoc.getEdgeTo(childLoc);
                Optional<State> nextState = computeNextState(previous, child, edge, pValueMap);
                if (nextState.isPresent() && visited.add(nextState.get())) {
                  stack.push(nextState.get());
                  lastEdgeStack.push(edge);
                }
              }
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  private Optional<State> computeNextState(
      State pPrevious,
      ARGState pChild,
      CFAEdge pEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    return Optional.of(State.of(pChild, pPrevious.testVector));
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions>
      getValueMap(CounterexampleInfo pCounterexampleInfo) {
    if (useModel && pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    }
    return ImmutableMultimap.of();
  }

  private TestVector addIndicesFromCPA(TestVector pTestVector, ARGState pArgState) {
    TestVector newTestVector = pTestVector;
    FluentIterable<HarnessState> harnessStates =
        AbstractStates.asIterable(pArgState).filter(HarnessState.class);
    for (HarnessState harnessState : harnessStates) {
      newTestVector =
          newTestVector.setExternPointersArrayLength(harnessState.getExternPointersArrayLength());
      for (ComparableFunctionDeclaration functionName : harnessState.getFunctionsWithIndices()) {
        for (int index : harnessState.getIndices(functionName)) {
          newTestVector = newTestVector.addPointerFunctionIndex(functionName, index);
        }
      }
    }
    return newTestVector;
  }

}
