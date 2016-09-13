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
 *  See the License for the specifi  c language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class FiducciaMattheysesAlgorithm {

  // TODO change variable naming

  private final Set<Integer> v1;

  private final Set<Integer> v2;

  private final PartialReachedSetDirectedGraph graph;

  private final double balanceCriterion;

  public FiducciaMattheysesAlgorithm(
      double pBalanceCriterion, Set<Integer> pV1, Set<Integer> pV2, PartialReachedSetDirectedGraph pGraph) {
    v1 = pV1;
    v2 = pV2;
    graph = pGraph;
    balanceCriterion = pBalanceCriterion;
  }

  private long computeGain(int pNode) {
    Set<Integer> src = v1.contains(pNode) ? v1 : v2;
    Set<Integer> dest = src == v1 ? v2 : v1;
    long dV1 = graph.getNumEdgesBetween(pNode, dest);
    long dV2 = graph.getNumEdgesBetween(pNode, src);
    return dV1 - dV2;
  }

  private void initDataStructures(
      Set<Integer> pV,
      TreeMap<Long, LinkedList<Integer>> pBuckets,
      HashMap<Integer, Long> pGain,
      HashMap<Integer, Boolean> lock) {
    for(Integer i : pV) {
      lock.put(i, false);
      long g = computeGain(i);
      pGain.put(i, g);
      if(!pBuckets.containsKey(g)) {
        pBuckets.put(g, new LinkedList<Integer>());
      }
      pBuckets.get(g).addFirst(i);
    }
  }

  private Optional<Pair<Long, TreeMap<Long, LinkedList<Integer>>>> tryFindBestGainWithNonEmptyBucket(
      final TreeMap<Long, LinkedList<Integer>> bucket) {
    return bucket
        .descendingKeySet()
        .stream()
        .filter(pLong -> !bucket.get(pLong).isEmpty())
        .findFirst()
        .map(bestGain -> Pair.of(bestGain, bucket));
  }

  // TODO make this configurable
  private boolean isBalanced(int pSizeP1, int pSizeP2) {
    int min = Math.min(pSizeP1, pSizeP2);
    if(min <= 0) {
      return false;
    }
    return Math.max(pSizeP1, pSizeP2)/(double)min <= balanceCriterion;
  }

  private Optional<Pair<Long, TreeMap<Long, LinkedList<Integer>>>> tryPickBestGain(
      final TreeMap<Long, LinkedList<Integer>> bucket1,
      final TreeMap<Long, LinkedList<Integer>> bucket2) {
    Optional<Pair<Long, TreeMap<Long, LinkedList<Integer>>>> bestV1Gain = tryFindBestGainWithNonEmptyBucket(bucket1);
    Optional<Pair<Long, TreeMap<Long, LinkedList<Integer>>>> bestV2Gain = tryFindBestGainWithNonEmptyBucket(bucket2);
    if(!bestV2Gain.isPresent()) {
      return bestV1Gain;
    }
    else if(!bestV1Gain.isPresent()) {
      return bestV2Gain;
    }
    else {
      if(bestV1Gain.get().getFirst() > bestV2Gain.get().getFirst() && isBalanced(v1.size()-1, v2.size()+1)) {
        return bestV1Gain;
      }
      else if(bestV1Gain.get().getFirst() <= bestV2Gain.get().getFirst() && isBalanced(v1.size()+1, v2.size()-1)) {
        // TODO handling equal case separately can be favourable
        return bestV2Gain;
      } else {
        return Optional.empty();
      }
    }
  }

  private int pollNodeFromBucketByGain(Long gain, final TreeMap<Long, LinkedList<Integer>> bucket) {
    // get first node from list in bucket
    // TODO implement alternative strategies
    return bucket.get(gain).poll();
  }

  private void updateGain(
      int pNode,
      final TreeMap<Long, LinkedList<Integer>> pBucket,
      HashMap<Integer, Long> pGain,
      long pNewGain) {
    boolean success = pBucket.get(pGain.get(pNode)).removeFirstOccurrence(pNode);
    assert(success);
    if(!pBucket.containsKey(pNewGain)) {
      pBucket.put(pNewGain, new LinkedList<Integer>());
    }
    pBucket.get(pNewGain).add(pNode);
    pGain.put(pNode, pNewGain);
  }

  private void updateNeighbors(
      int node,
      TreeMap<Long, LinkedList<Integer>> v1Buckets,
      TreeMap<Long, LinkedList<Integer>> v2Buckets,
      HashMap<Integer, Long> gain,
      HashMap<Integer, Boolean> lock) {
    Set<Integer> neighbors = new HashSet<>();
    neighbors.addAll(graph.getAdjacencyList().get(node));
    neighbors.addAll(graph.getPredecessorsOf(node));
    for(int n : neighbors) {
      boolean nInV1 = v1.contains(n);
      boolean nInV2 = v2.contains(n);
      if((nInV1 || nInV2) && !lock.get(n)) {
        long updatedGain = gain.get(n);
        if ((nInV1 && v1.contains(node)) || (nInV2 && v2.contains(node))) {
          updatedGain += 1;
        } else {
          updatedGain -= 1;
        }
        if(nInV1) {
          updateGain(n, v1Buckets, gain, updatedGain);
        } else {
          updateGain(n, v2Buckets, gain, updatedGain);
        }
      }
    }
  }

  public long improvePartitioning() {
    LinkedList<Integer> moved = new LinkedList<>();
    LinkedList<Long> cutSizes = new LinkedList<>();
    TreeMap<Long, LinkedList<Integer>> v1Buckets = new TreeMap<>();
    TreeMap<Long, LinkedList<Integer>> v2Buckets = new TreeMap<>();
    HashMap<Integer, Long> gain = new HashMap<>();
    HashMap<Integer, Boolean> lock = new HashMap<>();

      /* Initialize all the stuff */
    initDataStructures(v1, v1Buckets, gain, lock);
    initDataStructures(v2, v2Buckets, gain, lock);
    cutSizes.add(graph.getNumEdgesBetween(v1, v2));
    int iterationWithSmallestCutSize = 0;
    long smallestCutSize = cutSizes.get(0);

      /* Start algorithm */
    for(int i = 1; i < v1.size() + v2.size(); i++) {
      Optional<Pair<Long, TreeMap<Long, LinkedList<Integer>>>> gainAndBuckets = tryPickBestGain(v1Buckets, v2Buckets);
      if(!gainAndBuckets.isPresent()) {
        break;
      }
      long g = gainAndBuckets.get().getFirst();
      int node = pollNodeFromBucketByGain(g, gainAndBuckets.get().getSecond());
      lock.put(node, true);

      updateNeighbors(node, v1Buckets, v2Buckets, gain, lock);

        /* update log */
      long newCutSize = cutSizes.getLast() - g;
      assert(newCutSize >= 0);

      if(newCutSize < smallestCutSize) {
        iterationWithSmallestCutSize = i;
        smallestCutSize = newCutSize;
      }
      moved.add(node);
      cutSizes.addLast(newCutSize);
    }

      /* move nodes until best cut size reached */
    int i = 1;
    for(int node : moved) {
      if(i > iterationWithSmallestCutSize) {
        break;
      }
      if(v1.contains(node)) {
        v1.remove(node);
        v2.add(node);
      } else {
        v2.remove(node);
        v1.add(node);
      }
      i++;
    }
    return cutSizes.getFirst()-smallestCutSize;
  }

}
