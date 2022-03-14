// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableSubgraphReachedSetView;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final AbstractBAMCPA bamCpa;
  private final ARGPath path;
  private final TimerWrapper removeCachedSubtreeTimer;

  /**
   * Constructor for a ReachedSet containing all states, from where the last state of the path is
   * reachable. The ReachedSet might contain multiple paths to the same last state. We do not
   * guarantee that branches that do not lead to the last state are part of the ReachedSet.
   */
  public BAMReachedSet(
      AbstractBAMCPA cpa,
      ARGReachedSet pMainReachedSet,
      ARGPath pPath,
      TimerWrapper pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    bamCpa = cpa;
    path = pPath;
    removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;

    assert path.getFirstState().getSubgraph().toSet().containsAll(path.asStatesList())
        : "path should traverse reachable states";
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new UnmodifiableSubgraphReachedSetView(
        path, s -> super.asReachedSet().getPrecision(super.asReachedSet().getLastState()));
    // TODO do we really need the target-precision for refinements and not the actual one?
  }

  @Override
  public void removeSubtree(
      ARGState element, Precision newPrecision, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    removeSubtree(element, ImmutableList.of(newPrecision), ImmutableList.of(pPrecisionType));
  }

  @Override
  public void removeSubtree(
      ARGState element,
      List<Precision> newPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    Preconditions.checkArgument(newPrecisions.size() == pPrecisionTypes.size());
    assert path.getFirstState().getSubgraph().contains(element);
    final ARGSubtreeRemover argSubtreeRemover;
    if (bamCpa.useCopyOnWriteRefinement()) {
      argSubtreeRemover = new ARGCopyOnWriteSubtreeRemover(bamCpa, removeCachedSubtreeTimer);
    } else {
      argSubtreeRemover = new ARGInPlaceSubtreeRemover(bamCpa, removeCachedSubtreeTimer);
    }
    argSubtreeRemover.removeSubtree(delegate, path, element, newPrecisions, pPrecisionTypes);

    // post-processing, cleanup data-structures.
    // We remove all states reachable from 'element'. This step is not precise,
    // because sub-reached-sets might be changed and we do not remove the corresponding states.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    // We can ignore waitlist-updates and coverage here, because there is no coverage in a BAM-CEX.
    for (ARGState state : element.getSubgraph()) {
      state.removeFromARG();
    }
  }

  @Override
  public void removeSubtree(ARGState state) throws InterruptedException {
    removeSubtree(state, ImmutableList.of(), ImmutableList.of());
  }

  @Override
  public String toString() {
    return "BAMReachedSet {{" + asReachedSet().asCollection() + "}}";
  }
}
