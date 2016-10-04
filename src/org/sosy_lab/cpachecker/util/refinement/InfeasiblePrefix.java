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
package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class InfeasiblePrefix {

  private final ARGPath prefix;

  private final List<Set<String>> interpolantSequence;

  private final List<BooleanFormula> pathFormulas;

  private InfeasiblePrefix(final ARGPath pInfeasiblePrefix,
      final List<Set<String>> pSimpleInterpolantSequence) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = null;
  }

  private InfeasiblePrefix(final ARGPath pInfeasiblePrefix,
      final List<Set<String>> pSimpleInterpolantSequence,
      final List<BooleanFormula> pPathFormulas) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = pPathFormulas;
  }

  public static InfeasiblePrefix buildForPredicateDomain(final RawInfeasiblePrefix pRawInfeasiblePrefix,
      final FormulaManagerView pFmgr) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (BooleanFormula itp : pRawInfeasiblePrefix.interpolantSequence) {
      simpleInterpolantSequence.add(pFmgr.extractVariableNames(pFmgr.uninstantiate(itp)));
    }

    return new InfeasiblePrefix(pRawInfeasiblePrefix.prefix,
        simpleInterpolantSequence,
        pRawInfeasiblePrefix.pathFormulas);
  }

  public static InfeasiblePrefix buildForValueDomain(final ARGPath pInfeasiblePrefix,
      final List<ValueAnalysisInterpolant> pInterpolantSequence) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (ValueAnalysisInterpolant itp : pInterpolantSequence) {
      simpleInterpolantSequence.add(
          FluentIterable.from(itp.getMemoryLocations())
              .transform(MemoryLocation::getAsSimpleString)
              .toSet());
    }

    return new InfeasiblePrefix(pInfeasiblePrefix, simpleInterpolantSequence);
  }

  public Set<String> extractSetOfIdentifiers() {
    return ImmutableSet.copyOf(Iterables.concat(interpolantSequence));
  }

  public int getNonTrivialLength() {
    return FluentIterable.from(interpolantSequence).filter(Predicates.not(Set::isEmpty)).size();
  }

  public int getDepthOfPivotState() {
    int depth = 0;

    for (Set<String> itp : interpolantSequence) {
      if(!itp.isEmpty()) {
        return depth;
      }

      depth++;
    }

    // For the predicate analysis (with block size > 1), it can happen
    // that there are only trivial interpolants available, i.e., an
    // immediate change from [true] to [false] in the interpolant sequence.
    // So, only for the predicate analysis (<=> pathFormulas != null),
    // return the current depth in such a scenario
    if (pathFormulas != null) {
      return depth;
    }

    // for the value analysis, this must never be reached
    throw new AssertionError("There must be at least one non-trivial interpolant along the prefix.");
  }

  public ARGPath getPath() {
    return prefix;
  }

  public List<BooleanFormula> getPathFormulae() {
    return pathFormulas;
  }

  public static class RawInfeasiblePrefix {

    private final ARGPath prefix;
    private final List<BooleanFormula> interpolantSequence;
    private final List<BooleanFormula> pathFormulas;

    public RawInfeasiblePrefix(final ARGPath pInfeasiblePrefix,
        final List<BooleanFormula> pInterpolantSequence,
        final List<BooleanFormula> pPathFormulas) {

      this.prefix = pInfeasiblePrefix;
      this.interpolantSequence = pInterpolantSequence;
      this.pathFormulas = pPathFormulas;
    }
  }
}
