/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Utility class to check validity of all reached-sets when using BAM.
 *
 * <p>Just a collection of assertions on BAM data structures.
 */
public class BAMReachedSetValidator {

  public static boolean validateData(
      BAMDataManager data, BlockPartitioning partitioning, ARGReachedSet mainReachedSet) {

    ARGState mainEntry = (ARGState) mainReachedSet.asReachedSet().getFirstState();
    assert mainEntry.getParents().isEmpty();

    // for recursive programs, we use also reduce the main-block.
    // assert !data.hasInitialState(mainEntry) : "main entry should not be reduced";

    Collection<ReachedSet> reachedSets = data.getCache().getAllCachedReachedStates();

    assert validateReachedSet(
        mainEntry.getSubgraph(), mainReachedSet.asReachedSet(), data, partitioning, reachedSets);
    assert !reachedSets.contains(mainReachedSet.asReachedSet());
    for (ReachedSet rs : reachedSets) {
      assert validateReachedSet(
          ((ARGState) rs.getFirstState()).getSubgraph(), rs, data, partitioning, reachedSets);
    }

    return true;
  }

  @SuppressWarnings("unused")
  private static boolean validateReachedSet(
      Set<ARGState> subgraph,
      UnmodifiableReachedSet reachedSet,
      BAMDataManager data,
      BlockPartitioning partitioning,
      @Nullable Collection<ReachedSet> reachedSets) {
    assert subgraph.containsAll(reachedSet.asCollection());

    // check containment
    for (ARGState state : subgraph) {
      assert !state.isDestroyed();
      assert subgraph.containsAll(state.getChildren());
      assert subgraph.containsAll(state.getParents());
      assert subgraph.containsAll(state.getCoveredByThis());
      if (state.isCovered()) {
        assert subgraph.contains(state.getCoveringState());
        assert !reachedSet.contains(state);
      } else {
        assert reachedSet.contains(state);
      }
    }

    // check waitlist
    for (AbstractState state : reachedSet.getWaitlist()) {
      assert reachedSet.contains(state);
    }

    for (ARGState state : subgraph) {
      CFANode node = AbstractStates.extractLocation(state);
      if (partitioning.isCallNode(node)) {
        if (data.hasInitialState(state)) {
          for (ARGState child : state.getChildren()) {
            assert (data.hasExpandedState(child))
                : "child of non-reduced initial state should be expanded: " + child;
            ARGState reducedChild = (ARGState) data.getReducedStateForExpandedState(child);
            if (reducedChild.isDestroyed()) {
              // do not touch!
            } else {
              // there are assertions inside the call
              ReachedSet subRs = data.getReachedSetForInitialState(state, reducedChild);
              // if (reachedSets != null) {
              // assert reachedSets.contains(subRs)
              // : "Reached-set not found: " + subRs.getFirstState();
              // }
            }
          }
        }
      }
      if (partitioning.isReturnNode(node)) {
        for (AbstractState tmp : data.getExpandedStatesList(state)) {
          assert data.hasInitialState(Iterables.getOnlyElement(((ARGState) tmp).getParents()))
              : "single parent of expanded state should be non-reduced initial state: " + state;
        }
      }
    }
    return true;
  }
}
