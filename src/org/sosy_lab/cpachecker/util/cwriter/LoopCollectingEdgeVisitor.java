/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


@Options(prefix="cwriter.withLoops")
public class LoopCollectingEdgeVisitor implements EdgeVisitor {

  enum LoopDetectionStrategy {
    ALL_LOOPS, ONLY_LAST_LOOP;
  }

  @Option(toUppercase=true,
        description="Option to change the behaviour of the loop detection for"
            + " generating the Counterexample-C-Code that will probably be used to generate"
            + " invariants. Note that last loop means the first loop encountered when"
            + " backwards traversing the given ARGPath, thus, the last loop may contain"
            + " other loops, which are in turn also counted to the last loop.", secure=true)
  private LoopDetectionStrategy loopDetectionStrategy = LoopDetectionStrategy.ALL_LOOPS;

  private final LoopStructure loopStructure;
  private final List<Pair<CFAEdge, ARGState>> cfaPath = new ArrayList<>();
  private final Deque<Loop> loopStack = new ArrayDeque<>();
  private final Map<Loop, Set<ARGState>> relevantLoops = new LinkedHashMap<>();
  private final List<Loop> finishedLoops = new ArrayList<>();
  private boolean lastLoopFound = false;

  public LoopCollectingEdgeVisitor(LoopStructure pLoopStructure, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    loopStructure = pLoopStructure;
  }

  /**
   * Resets the state of this visitor, such that it works as if it was created newly.
   */
  public void reset() {
    cfaPath.clear();
    loopStack.clear();
    relevantLoops.clear();
    finishedLoops.clear();
    lastLoopFound = false;
  }

  @Override
  public void visit(ARGState childElement, CFAEdge edge, Stack<FunctionBody> functionStack) {
    cfaPath.add(Pair.of(edge, childElement));
  }

  public Map<Loop, Set<ARGState>> getRelevantLoops() {
    ListIterator<Pair<CFAEdge, ARGState>> cfaIterator = cfaPath.listIterator(cfaPath.size());
    CFAEdge edge = cfaPath.get(cfaPath.size()-1).getFirst();
    ARGState state = cfaPath.get(cfaPath.size()-2).getSecond();

    // Creates the initial loopStack, as seen from the error state's location
    handleLoopStack(edge, state);

    // now backwards traverse the list
    while (cfaIterator.hasPrevious()) {
      // fetch current arg path element (only cfa necessary, therefore only this
      // one is here)
      Pair<CFAEdge, ARGState> tmp = cfaIterator.previous();
      edge = tmp.getFirst();
      if(cfaIterator.hasPrevious()) {
        state = cfaIterator.previous().getSecond();
        cfaIterator.next();
      } else {
        break;
      }

      // check if the cfaNode has more than one outgoing edges, and if yes
      // if one of these is a functionsummary edge, we want to skip
      // all states until the next state in the current function if it is so
      if (edge instanceof CFunctionReturnEdge) {
        CFANode beforeFunctionCall = ((CFunctionReturnEdge) edge).getSummaryEdge().getPredecessor();
        while (cfaIterator.hasPrevious()) {
          tmp = cfaIterator.previous();
          if (tmp.getFirst().getPredecessor() == beforeFunctionCall) {
            edge = tmp.getFirst();
            if(cfaIterator.hasPrevious()) {
              state = cfaIterator.previous().getSecond();
              cfaIterator.next();
            }
            break;
          }
        }
      }

      handleLoopStack(edge, state);

      // if we have found the last loop and all belonging states,
      // so we can skip further computation here
      if (loopDetectionStrategy == LoopDetectionStrategy.ONLY_LAST_LOOP
          && lastLoopFound && loopStack.isEmpty()) {
        break;
      }
    }

    return relevantLoops;
  }

  /**
   * Updates the loop information in loopStack of a given edge
   */
  private void handleLoopStack(CFAEdge edge, ARGState state) {
    // if the edge is null we can just add the ARGState to the current topmost
    // loop if there is one, the loopstack won't change in such situations
    if (edge == null) {
      if (!loopStack.isEmpty()) {
        List<Loop> loops = getLoopsOfNode(loopStructure, extractLocation(state));
        // ArrayDeque and List can unfortunately not be compared completely with
        // equals, therefore we check at least the size
        assert loops.size() == loopStack.size();

        for (Loop loop : loops) {
          relevantLoops.get(loop).add(state);
        }
      }
      return;
    }

    CFANode predecessor = edge.getPredecessor();

    // remove all loops which we are not in currently
    while (!loopStack.isEmpty() && !loopStack.peek().getLoopNodes().contains(predecessor)) {
      finishedLoops.add(loopStack.pop());
    }

    boolean isInLoop = isInAnyLoop(loopStructure, predecessor);

    List<Loop> loops = getLoopsOfNode(loopStructure, predecessor);

    if (!loopStack.isEmpty()) {
      relevantLoops.get(loopStack.peek()).add(state);
      int startPushingIndex = loops.size();
      while (loops.get(startPushingIndex-1) != loopStack.peek()) {
        startPushingIndex--;
      }
      for (int i = startPushingIndex; i < loops.size(); i++) {
        Loop actLoop = loops.get(i);
        loopStack.push(actLoop);
        Set<ARGState> states = new HashSet<>();
        states.add(state);
        relevantLoops.put(actLoop, states);
      }

      // loopstack is empty, so we only need to push something
      // on the stack if we need it
    } else if (lastLoopFound && loopDetectionStrategy == LoopDetectionStrategy.ONLY_LAST_LOOP) {
      return;

      // we either need all loops because there were no before or because of
      // the heuristic
    } else {
      for (int i = 0; i < loops.size(); i++) {
        Loop actLoop = loops.get(i);
        loopStack.push(actLoop);
        Set<ARGState> states = new HashSet<>();
        states.add(state);
        relevantLoops.put(actLoop, states);
      }
    }

    if (!lastLoopFound && isInLoop) {
      lastLoopFound = true;
    }
  }

  /**
   * Checks if a given CFANode is part of any loop.
   */
  static boolean isInAnyLoop(LoopStructure loopStructure, final CFANode node) {
    return from(loopStructure.getAllLoops())
        .anyMatch(pInput -> pInput.getLoopNodes().contains(node));
  }

  /**
   * Returns the Loops in which the given node is located from the outermost
   * to the innermost loop.
   */
  static List<Loop> getLoopsOfNode(LoopStructure loopStructure, final CFANode node) {
    return from(loopStructure.getAllLoops())
        .filter(pInput -> pInput.getLoopNodes().contains(node))
        .toSortedList(
            (loop1, loop2) -> {
              return isOuterLoopOf(loop1, loop2) ? -1 : 1;
            });
  }

  static boolean isOuterLoopOf(Loop outer, Loop inner) {
    return outer.getLoopNodes().containsAll(inner.getLoopNodes());
  }
}
