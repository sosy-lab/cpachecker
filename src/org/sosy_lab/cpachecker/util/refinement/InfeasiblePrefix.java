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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;


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

  public static InfeasiblePrefix buildForPredicateDomain(final ARGPath pInfeasiblePrefix,
      final List<BooleanFormula> pInterpolantSequence,
      final List<BooleanFormula> pPathFormulas,
      final FormulaManagerView pFmgr) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (BooleanFormula itp : pInterpolantSequence) {
      simpleInterpolantSequence.add(pFmgr.extractVariableNames(pFmgr.uninstantiate(itp)));
    }

    return new InfeasiblePrefix(pInfeasiblePrefix,
        simpleInterpolantSequence,
        pPathFormulas);
  }

  public static InfeasiblePrefix buildForValueDomain(final ARGPath pInfeasiblePrefix,
      final List<ValueAnalysisInterpolant> pInterpolantSequence) {

    List<Set<String>> simpleInterpolantSequence = new ArrayList<>();
    for (ValueAnalysisInterpolant itp : pInterpolantSequence) {
      simpleInterpolantSequence.add(FluentIterable.from(itp.getMemoryLocations()).transform(MemoryLocation.FROM_MEMORYLOCATION_TO_STRING).toSet());
    }

    return new InfeasiblePrefix(pInfeasiblePrefix, simpleInterpolantSequence);
  }

  public Set<String> extractSetOfIdentifiers() {
    return FluentIterable.from(interpolantSequence).transformAndConcat(new Function<Set<String>, Iterable<String>>() {
      @Override
      public Iterable<String> apply(Set<String> itp) {
        return itp;
      }}).toSet();
  }

  public int getNonTrivialLength() {
    return FluentIterable.from(interpolantSequence).filter(new Predicate<Set<String>>() {
      @Override
      public boolean apply(Set<String> pInput) {
        return !pInput.isEmpty();
      }}).size();
  }

  public int getDepthOfPivotState() {
    int depth = 0;

    for (Set<String> itp : interpolantSequence) {
      if(!itp.isEmpty()) {
        return depth;
      }

      depth++;
    }
    assert false : "There must be at least one trivial interpolant along the prefix";

    return -1;
  }

  public ARGPath getPath() {
    return prefix;
  }

  public List<BooleanFormula> getPathFormulae() {
    return pathFormulas;
  }
}
