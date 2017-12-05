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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.HashSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class ARGMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMerge;
  private final AbstractDomain wrappedDomain;

  @Option(
      secure = true,
      name = "cpa.arg.mergeOnWrappedSubsumption",
      description = "If this option is enabled, ARG states will also be merged if the first wrapped state is \n"
          + " subsumed by the second wrapped state (and the parents are not yet subsumed).")
  private boolean mergeOnWrappedSubsumption = false;

  public ARGMergeJoin(MergeOperator pWrappedMerge, AbstractDomain pWrappedDomain, Configuration config)
      throws InvalidConfigurationException {
    wrappedMerge = pWrappedMerge;
    wrappedDomain = pWrappedDomain;
    config.inject(this);
  }

  @Override
  public AbstractState merge(AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision) throws CPAException, InterruptedException {

    ARGState argElement1 = (ARGState)pElement1;
    ARGState argElement2 = (ARGState)pElement2;

    assert !argElement1.isCovered() : "Trying to merge covered element " + argElement1;

    if (!argElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pElement2;
    }

    if (argElement1.getMergedWith() != null) {
      // element was already merged into another element, don't try to widen argElement2
      // TODO In the optimal case (if all merge & stop operators as well as the reached set partitioning fit well together)
      // this case shouldn't happen, but it does sometimes (at least with ExplicitCPA+FeatureVarsCPA).
      return pElement2;
    }

    AbstractState wrappedState1 = argElement1.getWrappedState();
    AbstractState wrappedState2 = argElement2.getWrappedState();
    AbstractState retElement = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);

    boolean continueMerge = !retElement.equals(wrappedState2);
    if (mergeOnWrappedSubsumption) {
      HashSet<ARGState> parents1 = new HashSet<>(argElement1.getParents());
      HashSet<ARGState> parents2 = new HashSet<>(argElement2.getParents());
      continueMerge = continueMerge ||
          ((!parents2.containsAll(parents1) && wrappedDomain.isLessOrEqual(wrappedState1,wrappedState2)));
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
