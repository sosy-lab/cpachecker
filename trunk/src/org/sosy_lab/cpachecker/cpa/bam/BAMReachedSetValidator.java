// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
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
        mainEntry.getSubgraph().toSet(),
        mainReachedSet.asReachedSet(),
        data,
        partitioning,
        reachedSets);
    assert !reachedSets.contains(mainReachedSet.asReachedSet());
    for (ReachedSet rs : reachedSets) {
      assert validateReachedSet(
          ((ARGState) rs.getFirstState()).getSubgraph().toSet(),
          rs,
          data,
          partitioning,
          reachedSets);
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
            assert data.hasExpandedState(child)
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
