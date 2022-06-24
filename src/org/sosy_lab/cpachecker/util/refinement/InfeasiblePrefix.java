// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InfeasiblePrefix {

  private final ARGPath prefix;

  private final ImmutableList<ImmutableSet<String>> interpolantSequence;

  private final ImmutableList<BooleanFormula> pathFormulas;

  private InfeasiblePrefix(
      final ARGPath pInfeasiblePrefix,
      final ImmutableList<ImmutableSet<String>> pSimpleInterpolantSequence) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = null;
  }

  private InfeasiblePrefix(
      final ARGPath pInfeasiblePrefix,
      final ImmutableList<ImmutableSet<String>> pSimpleInterpolantSequence,
      final ImmutableList<BooleanFormula> pPathFormulas) {

    prefix = pInfeasiblePrefix;
    interpolantSequence = pSimpleInterpolantSequence;

    pathFormulas = pPathFormulas;
  }

  public static InfeasiblePrefix buildForPredicateDomain(
      final RawInfeasiblePrefix pRawInfeasiblePrefix, final FormulaManagerView pFmgr) {

    ImmutableList.Builder<ImmutableSet<String>> simpleInterpolantSequence = ImmutableList.builder();
    for (BooleanFormula itp : pRawInfeasiblePrefix.interpolantSequence) {
      simpleInterpolantSequence.add(
          ImmutableSet.copyOf(pFmgr.extractVariableNames(pFmgr.uninstantiate(itp))));
    }

    return new InfeasiblePrefix(
        pRawInfeasiblePrefix.prefix,
        simpleInterpolantSequence.build(),
        pRawInfeasiblePrefix.pathFormulas);
  }

  public static InfeasiblePrefix buildForValueDomain(
      final ARGPath pInfeasiblePrefix, final List<ValueAnalysisInterpolant> pInterpolantSequence) {

    ImmutableList.Builder<ImmutableSet<String>> simpleInterpolantSequence = ImmutableList.builder();
    for (ValueAnalysisInterpolant itp : pInterpolantSequence) {
      simpleInterpolantSequence.add(
          transformedImmutableSetCopy(
              itp.getMemoryLocations(), MemoryLocation::getExtendedQualifiedName));
    }

    return new InfeasiblePrefix(pInfeasiblePrefix, simpleInterpolantSequence.build());
  }

  public ImmutableSet<String> extractSetOfIdentifiers() {
    return ImmutableSet.copyOf(Iterables.concat(interpolantSequence));
  }

  public int getNonTrivialLength() {
    return FluentIterable.from(interpolantSequence).filter(Predicates.not(Set::isEmpty)).size();
  }

  public int getDepthOfPivotState() {
    int depth = 0;

    for (Set<String> itp : interpolantSequence) {
      if (!itp.isEmpty()) {
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
    throw new AssertionError(
        "There must be at least one non-trivial interpolant along the prefix.");
  }

  public ARGPath getPath() {
    return prefix;
  }

  public ImmutableList<BooleanFormula> getPathFormulae() {
    return pathFormulas;
  }

  public static class RawInfeasiblePrefix {

    private final ARGPath prefix;
    private final ImmutableList<BooleanFormula> interpolantSequence;
    private final ImmutableList<BooleanFormula> pathFormulas;

    public RawInfeasiblePrefix(
        final ARGPath pInfeasiblePrefix,
        final ImmutableList<BooleanFormula> pInterpolantSequence,
        final ImmutableList<BooleanFormula> pPathFormulas) {

      prefix = pInfeasiblePrefix;
      interpolantSequence = pInterpolantSequence;
      pathFormulas = pPathFormulas;
    }
  }
}
