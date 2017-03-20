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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.io.PrintStream;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Preconditions;


public abstract class GenericSinglePathRefiner extends
    WrappedConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>, Pair<ExtendedARGPath, ExtendedARGPath>>  {

  Timer totalTimer = new Timer();
  int numberOfRefinements = 0;

  public GenericSinglePathRefiner(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper) {
    super(pWrapper);
  }

  @Override
  public final RefinementResult performRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput) throws CPAException, InterruptedException {
    totalTimer.start();

    try {
      ExtendedARGPath firstPath = pInput.getFirst();
      ExtendedARGPath secondPath = pInput.getSecond();

      PredicatePrecision completePrecision = PredicatePrecision.empty();
      //Refine paths separately
      RefinementResult result = refinePath(firstPath);
      if (result.isFalse()) {
        return result;
      }
      PredicatePrecision precision = result.getPrecision();
      if (precision != null) {
        completePrecision = completePrecision.mergeWith(precision);
      }
      result = refinePath(secondPath);
      precision = result.getPrecision();
      if (precision != null) {
        completePrecision = completePrecision.mergeWith(precision);
      }
      if (result.isFalse()) {
        result.addPrecision(completePrecision);
        return result;
      }
      result = wrappedRefiner.performRefinement(pInput);
      result.addPrecision(completePrecision);
      return result;
    } finally {
      totalTimer.stop();
    }
  }

  private RefinementResult refinePath(ExtendedARGPath path) throws CPAException, InterruptedException {
    Preconditions.checkArgument(!path.isUnreachable(), "Path could not be unreachable here");

    if (path.isRefinedAsReachableBy(this)) {
      //Means that is is reachable, but other refiners declined it.
      //Now the pair changes. Do not refine it again.
      return RefinementResult.createTrue();
    }

    numberOfRefinements++;
    RefinementResult result = call(path);
    if (result.isTrue() || result.isUnknown()) {
      path.setAsTrueBy(this);
      return result;
    } else {
      path.setAsFalse();
      return result;
    }
  }

  @Override
  public final void printStatistics(PrintStream pOut) {
    pOut.println("--GenericSinglePathRefiner--");
    pOut.println("Timer for block:           " + totalTimer);
    pOut.println("Number of calls:           " + numberOfRefinements);
    printAdditionalStatistics(pOut);
    wrappedRefiner.printStatistics(pOut);
  }

  //ForOverride
  public void printAdditionalStatistics(PrintStream pOut) {}

  protected abstract RefinementResult call(ExtendedARGPath path) throws CPAException, InterruptedException;
}
