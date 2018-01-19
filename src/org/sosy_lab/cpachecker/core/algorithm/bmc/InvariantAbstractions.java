/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class InvariantAbstractions {

  private InvariantAbstractions() {
    // Utility class
  }

  private static class NoAbstraction<S extends CandidateInvariant>
      implements InvariantAbstraction<S, S> {

    private static NoAbstraction<CandidateInvariant> INSTANCE = new NoAbstraction<>();

    private NoAbstraction() {}

    @Override
    public S performAbstraction(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        PredicateAbstractionManager pPam,
        S pInvariant,
        Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
        Map<BooleanFormula, Object> pSuccessorViolationAssertionIds,
        Optional<BooleanFormula> pAssertedInvariants) {
      return pInvariant;
    }
  }

  @SuppressWarnings("unchecked")
  public static <S extends CandidateInvariant> InvariantAbstraction<S, S> noAbstraction() {
    return (NoAbstraction<S>) NoAbstraction.INSTANCE;
  }

  private static class InterpolatingAbstraction
      implements InvariantAbstraction<
          SingleLocationFormulaInvariant, SingleLocationFormulaInvariant> {

    private final AbstractionStrategy abstractionStrategy;

    private InterpolatingAbstraction(AbstractionStrategy pAbstractionStrategy) {
      abstractionStrategy = Objects.requireNonNull(pAbstractionStrategy);
    }

    @Override
    public SingleLocationFormulaInvariant performAbstraction(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        PredicateAbstractionManager pPam,
        SingleLocationFormulaInvariant pInvariant,
        Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
        Map<BooleanFormula, Object> pSuccessorViolationAssertionIds,
        Optional<BooleanFormula> pAssertedInvariants)
        throws SolverException, InterruptedException {
      BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
      CFANode location = pInvariant.getLocation();
      SingleLocationFormulaInvariant refinedInvariant;

      // There are two non-trivial cases we should consider (for efficiency):
      // Case 1: There was exactly one state, so we already have nice conjunction.
      // The conjunctive parts should have been pushed separately,
      // to the special handling of InterpolatingAbstractionSuccessorViolation.
      // We can detect this case by checking if there is more than one assertion id:
      if (pSuccessorViolationAssertionIds.size() > 1) {
        // The relevant assertion ids for interpolation are those where the formulas
        // are not in the set of state formulas:
        Collection<Object> invariantAssertionIds =
            Maps.filterKeys(
                    pSuccessorViolationAssertionIds,
                    formula -> !pStateViolationAssertions.keySet().contains(formula))
                .values();
        BooleanFormula interpolant = pProver.getInterpolant(invariantAssertionIds);
        interpolant = bfmgr.not(pFmgr.uninstantiate(interpolant));
        abstractionStrategy.refinePrecision(pPam, location, pFmgr.extractAtoms(interpolant, true));
        refinedInvariant =
            SingleLocationFormulaInvariant.makeLocationInvariant(location, interpolant, pFmgr);
      } else {

        // Case 2: There was more than one state (or zero states),
        // and so the violation formula is one big disjunction.
        // What we want to do now is pop this disjunction
        // and push (and interpolate for) each disjunctive component separately.
        // Since the disjunction was unsatisfiable,
        // each disjunctive component must be unsatisfiable, too:
        BooleanFormula interpolantDisjunction = bfmgr.makeFalse();
        if (!pStateViolationAssertions.isEmpty()) {
          if (pAssertedInvariants.isPresent()) {
            pProver.pop(); // Pop asserted invariants
          }
          pProver.pop(); // Pop the big disjunction
          if (pAssertedInvariants.isPresent()) {
            pProver.push(pAssertedInvariants.get()); // Put the invariants back on the stack
          }

          boolean firstIteration = true;
          for (Map.Entry<BooleanFormula, Collection<BooleanFormula>> entry :
              pStateViolationAssertions.asMap().entrySet()) {
            if (firstIteration) {
              firstIteration = false;
            } else {
              pProver.pop(); // Pop the previous state assertion
            }

            pProver.push(entry.getKey()); // Push the state assertion
            // Push the invariant-violation assertion
            Object invariantViolationAssertionId = pProver.push(bfmgr.and(entry.getValue()));
            if (!pProver.isUnsat()) {
              pProver.pop(); // Pop the invariant-violation assertion
              return pInvariant;
            }
            BooleanFormula interpolant =
                pProver.getInterpolant(Arrays.asList(invariantViolationAssertionId));
            interpolant = bfmgr.not(pFmgr.uninstantiate(interpolant));
            interpolantDisjunction = bfmgr.or(interpolantDisjunction, interpolant);

            pProver.pop(); // Pop the invariant-violation assertion
          }
          // We deliberately leave the last state assertion on the stack
          // so that at least the stack size is the same as before
        }
        if (!bfmgr.isTrue(interpolantDisjunction)) {
          abstractionStrategy.refinePrecision(
              pPam, location, Collections.singleton(interpolantDisjunction));
        }
        refinedInvariant =
            SingleLocationFormulaInvariant.makeLocationInvariant(
                location, interpolantDisjunction, pFmgr);
      }
      refinedInvariant =
          CandidateInvariantConjunction.ofSingleLocation(pInvariant, refinedInvariant);
      return refinedInvariant;
    }
  }

  static InvariantAbstraction<SingleLocationFormulaInvariant, SingleLocationFormulaInvariant>
      interpolatingAbstraction(AbstractionStrategy pAbstractionStrategy) {
    return new InterpolatingAbstraction(pAbstractionStrategy);
  }
}
