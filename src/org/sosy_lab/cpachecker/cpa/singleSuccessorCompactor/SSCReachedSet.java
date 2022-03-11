// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableSubgraphReachedSetView;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class SSCReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  /** some states of the reached-set are part of the path, i.e., as sampling points. */
  private final SSCPath path;

  SSCReachedSet(ARGReachedSet pReached, SSCPath pPath) {
    super(pReached);
    path = pPath;
    assert path.getFirstState().getSubgraph().toSet().containsAll(path.asStatesList())
        : "path should traverse reachable states";
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new UnmodifiableSubgraphReachedSetView(
        path, s -> super.asReachedSet().getPrecision(((SSCARGState) s).getSSCState()));
  }

  @Override
  public void removeSubtree(ARGState state) throws InterruptedException {
    removeSubtree(state, ImmutableList.of(), ImmutableList.of());
  }

  @Override
  public void removeSubtree(
      ARGState element, Precision newPrecision, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    removeSubtree(element, ImmutableList.of(newPrecision), ImmutableList.of(pPrecisionType));
  }

  /**
   * This method over-approximates the cut-point. It searches the latest original state and cuts
   * there, i.e., it might cut more than needed, but sufficiently enough to remove the property
   * violation. We assume that the precision gets stronger along all paths.
   */
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public void removeSubtree(
      ARGState cutState,
      List<Precision> newPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    Preconditions.checkArgument(newPrecisions.size() == pPrecisionTypes.size());
    assert path.getFirstState().getSubgraph().contains(cutState);

    // find latest original ssc-state
    SSCARGState sccState = (SSCARGState) cutState;
    while (sccState.getSSCState().getWrappedState() != sccState.getWrappedState()) {
      sccState = (SSCARGState) Iterables.getOnlyElement(sccState.getParents());
    }
    assert sccState.getSSCState().equals(((SSCARGState) cutState).getSSCState());

    // remove original state and its subtree
    super.removeSubtree(sccState.getSSCState(), newPrecisions, pPrecisionTypes);

    // post-processing, cleanup data-structures.
    // We remove all states reachable from 'cutState'. This step is not precise.
    // The only important step is to remove the last state of the reached-set.
    // We can ignore waitlist-updates and coverage here, because those things should not be needed.
    for (ARGState state : sccState.getSubgraph()) {
      state.removeFromARG();
    }
  }

  @Override
  public String toString() {
    return "SSCReachedSet {{" + asReachedSet().asCollection() + "}}";
  }
}
