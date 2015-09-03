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
package org.sosy_lab.cpachecker.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Some utilities for generic graphs.
 */
public class GraphUtils {

  private GraphUtils() { }

  /**
   * Project a graph to a subset of "relevant" nodes.
   * The result is a SetMultimap containing the successor relationships between all relevant nodes.
   * A pair of nodes (a, b) is in the SetMultimap,
   * if there is a path through the graph from a to b which does not pass through
   * any other relevant node.
   *
   * To get the predecessor relationship, you can use {@link Multimaps#invertFrom(com.google.common.collect.Multimap, com.google.common.collect.Multimap)}.
   *
   * @param root The start of the graph to project (always considered relevant).
   * @param isRelevant The predicate determining which nodes are in the resulting relationship.
   * @param successorFunction A function giving the direct successors of any node.
   * @param <N> The node type of the graph.
   */
  public static <N> SetMultimap<N, N> projectARG(final N root,
      final Function<? super N, ? extends Iterable<N>> successorFunction,
      Predicate<? super N> isRelevant) {

    isRelevant = Predicates.or(Predicates.equalTo(root),
                               isRelevant);

    SetMultimap<N, N> successors = HashMultimap.create();

    // Our state is a stack of pairs of todo items.
    // The first item of each pair is a relevant state,
    // for which we are looking for relevant successor states.
    // The second item is a state,
    // whose children should be handled next.
    Deque<Pair<N, N>> todo = new ArrayDeque<>();
    Set<N> visited = new HashSet<>();
    todo.push(Pair.of(root, root));

    while (!todo.isEmpty()) {
      final Pair<N, N> currentPair = todo.pop();
      final N currentPredecessor = currentPair.getFirst();
      final N currentState = currentPair.getSecond();

      if (!visited.add(currentState)) {
        continue;
      }

      for (N child : successorFunction.apply(currentState)) {
        if (isRelevant.apply(child)) {
          successors.put(currentPredecessor, child);

          todo.push(Pair.of(child, child));

        } else {
          todo.push(Pair.of(currentPredecessor, child));
        }
      }
    }

    return successors;
  }
}
