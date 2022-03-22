// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMerge;
  private final AbstractDomain wrappedDomain;
  private final boolean mergeOnWrappedSubsumption;

  public ARGMergeJoin(
      MergeOperator pWrappedMerge,
      AbstractDomain pWrappedDomain,
      boolean pMergeOnWrappedSubsumption) {
    wrappedMerge = pWrappedMerge;
    wrappedDomain = pWrappedDomain;
    mergeOnWrappedSubsumption = pMergeOnWrappedSubsumption;
  }

  @Override
  public AbstractState merge(AbstractState pElement1, AbstractState pElement2, Precision pPrecision)
      throws CPAException, InterruptedException {

    ARGState argElement1 = (ARGState) pElement1;
    ARGState argElement2 = (ARGState) pElement2;

    assert !argElement1.isCovered() : "Trying to merge covered element " + argElement1;

    if (!argElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pElement2;
    }

    if (argElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set
      // partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with
      // ExplicitCPA+FeatureVarsCPA).
      return pElement2;
    }

    AbstractState wrappedState1 = argElement1.getWrappedState();
    AbstractState wrappedState2 = argElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);

    boolean continueMerge = !retElement.equals(wrappedState2);
    if (mergeOnWrappedSubsumption) {
      Set<ARGState> parents1 = ImmutableSet.copyOf(argElement1.getParents());
      Set<ARGState> parents2 = ImmutableSet.copyOf(argElement2.getParents());
      continueMerge =
          continueMerge
              || (!parents2.containsAll(parents1)
                  && wrappedDomain.isLessOrEqual(wrappedState1, wrappedState2));
    }
    if (!continueMerge) {
      return pElement2;
    }

    ARGState mergedElement = new ARGState(retElement, null);

    // now replace argElement2 by mergedElement in ARG
    argElement2.replaceInARGWith(mergedElement);

    // and also replace argElement1 with it
    for (ARGState parentOfElement1 : argElement1.getParents()) {
      mergedElement.addParent(parentOfElement1);
    }

    // argElement1 is the current successor, it does not have any children yet and covered nodes yet
    assert argElement1.getChildren().isEmpty();
    assert argElement1.getCoveredByThis().isEmpty();

    // ARGElement1 will only be removed from ARG if stop(e1, reached) returns true.
    // So we can't actually remove it now, but we need to remember this later.
    argElement1.setMergedWith(mergedElement);
    return mergedElement;
  }
}
