// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.pdr.TotalTransitionRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class InvariantStrengthenings {

  private InvariantStrengthenings() {
    // Utility class
  }

  private static class NoAbstraction<S extends CandidateInvariant>
      implements InvariantStrengthening<S, S> {

    private static final NoAbstraction<CandidateInvariant> INSTANCE = new NoAbstraction<>();

    private NoAbstraction() {}

    @Override
    public S strengthenInvariant(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        PredicateAbstractionManager pPam,
        S pInvariant,
        AssertCandidate pAssertPredecessor,
        AssertCandidate pAssertSuccessorViolation,
        AssertCandidate pAssertCti,
        Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
        Optional<BooleanFormula> pAssertedInvariants,
        NextCti pNextCti) {
      return pInvariant;
    }
  }

  @SuppressWarnings("unchecked")
  public static <S extends CandidateInvariant> InvariantStrengthening<S, S> noStrengthening() {
    return (NoAbstraction<S>) NoAbstraction.INSTANCE;
  }

  private enum UnsatCoreBasedRefinement
      implements InvariantStrengthening<SymbolicCandiateInvariant, SymbolicCandiateInvariant> {
    INSTANCE;

    @Override
    public SymbolicCandiateInvariant strengthenInvariant(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        PredicateAbstractionManager pPam,
        SymbolicCandiateInvariant pInvariant,
        AssertCandidate pAssertPredecessor,
        AssertCandidate pAssertSuccessorViolation,
        AssertCandidate pAssertCti,
        Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
        Optional<BooleanFormula> pAssertedInvariants,
        NextCti pNextCti)
        throws SolverException, InterruptedException, CPATransferException {

      if (pStateViolationAssertions.isEmpty()) {
        return pInvariant;
      }

      Set<BooleanFormula> relevantLiterals = new LinkedHashSet<>();

      if (pAssertedInvariants.isPresent()) {
        pProver.pop(); // Pop asserted invariants
      }
      pProver.pop(); // Pop the big violation disjunction

      if (pAssertedInvariants.isPresent()) {
        pProver.push(pAssertedInvariants.orElseThrow()); // Put the invariants back on the stack
      }

      // Find the relevant literals for each disjunct
      for (Map.Entry<BooleanFormula, Collection<BooleanFormula>> entry :
          pStateViolationAssertions.asMap().entrySet()) {

        pProver.push(entry.getKey()); // Push the state assertion

        Collection<BooleanFormula> invariantAssertionComponents = entry.getValue();
        if (!determineRelevantLiterals(
            pProver, pFmgr, invariantAssertionComponents, relevantLiterals)) {
          // We deliberately leave the previous state assertion
          // so that the prover stack size matches,
          // which makes prover cleanup easier for the caller.
          return pInvariant;
        }
        pProver.pop(); // Pop the previous state assertion
      }

      // Now we need to ensure consecution still succeeds
      // or restore it by adding literals back
      return restoreConsecution(
          pProver,
          pFmgr,
          relevantLiterals,
          pInvariant,
          pAssertPredecessor,
          pAssertSuccessorViolation,
          pAssertCti,
          pAssertedInvariants,
          pNextCti);
    }

    private SymbolicCandiateInvariant restoreConsecution(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        Set<BooleanFormula> pChosenLiterals,
        SymbolicCandiateInvariant pInvariant,
        AssertCandidate pAssertPredecessor,
        AssertCandidate pAssertSuccessorViolation,
        AssertCandidate pAssertCti,
        Optional<BooleanFormula> pAssertedInvariants,
        NextCti pNextCti)
        throws InterruptedException, CPATransferException, SolverException {
      BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();

      Map<BooleanFormula, SymbolicCandiateInvariant> remainingLiterals = new LinkedHashMap<>();

      for (BooleanFormula literal :
          SymbolicCandiateInvariant.getConjunctionOperands(
              pFmgr, pInvariant.getPlainFormula(pFmgr), true)) {
        if (!pChosenLiterals.contains(literal)) {
          SymbolicCandiateInvariant symbolicLiteral =
              SymbolicCandiateInvariant.makeSymbolicInvariant(
                  pInvariant.getApplicableLocations(), pInvariant.getStateFilter(), literal, pFmgr);
          remainingLiterals.put(literal, symbolicLiteral);
        }
      }

      boolean restored = remainingLiterals.isEmpty();

      SymbolicCandiateInvariant refinedInvariant =
          SymbolicCandiateInvariant.makeSymbolicInvariant(
              pInvariant.getApplicableLocations(),
              pInvariant.getStateFilter(),
              bfmgr.not(bfmgr.and(pChosenLiterals)),
              pFmgr);

      // If all literals were added already, we can immediately return,
      // we just need to restore the stack size by pushing a dummy
      if (remainingLiterals.isEmpty()) {
        pProver.push(bfmgr.makeTrue());
        return refinedInvariant;
      }

      if (pAssertedInvariants.isPresent()) {
        pProver.pop(); // Pop asserted invariants
      }
      pProver.pop(); // Pop the candidate assertion
      if (pAssertedInvariants.isPresent()) {
        pProver.push(pAssertedInvariants.orElseThrow()); // Put the invariants back on the stack
      }

      while (!restored) {
        // Check consecution
        pProver.push(pAssertPredecessor.assertCandidate(refinedInvariant));
        pProver.push(pAssertSuccessorViolation.assertCandidate(refinedInvariant));

        // If consecution succeeds, we are done
        if (pProver.isUnsat()) {
          restored = true;
        } else {
          // If consecution does not succeed, check why (cti)
          // so that we can try to fix the problem
          Optional<CounterexampleToInductivity> cti = pNextCti.getNextCti();
          if (!cti.isPresent()) {
            return pInvariant;
          }

          // Find and add all literals whose negation is implied by the model.
          // This should be rather cheap to do.
          SymbolicCandiateInvariant assertableCti =
              SymbolicCandiateInvariant.makeSymbolicInvariant(
                  pInvariant.getApplicableLocations(),
                  pInvariant.getStateFilter(),
                  cti.orElseThrow().getFormula(pFmgr),
                  pFmgr);
          pProver.push(pAssertCti.assertCandidate(assertableCti));

          Iterator<Map.Entry<BooleanFormula, SymbolicCandiateInvariant>> remainingLiteralIterator =
              remainingLiterals.entrySet().iterator();
          boolean isUnsat = false;
          do {
            Map.Entry<BooleanFormula, SymbolicCandiateInvariant> remainingLiteral =
                remainingLiteralIterator.next();
            pProver.push(pAssertPredecessor.assertCandidate(remainingLiteral.getValue()));
            isUnsat = pProver.isUnsat();
            pProver.pop(); // Pop the literal
            if (isUnsat) {
              remainingLiteralIterator.remove();
              pChosenLiterals.add(remainingLiteral.getKey());
            }
          } while (!isUnsat && remainingLiteralIterator.hasNext());

          pProver.pop(); // Pop the model

          if (!isUnsat) {
            return pInvariant;
          }

          refinedInvariant =
              SymbolicCandiateInvariant.makeSymbolicInvariant(
                  pInvariant.getApplicableLocations(),
                  pInvariant.getStateFilter(),
                  bfmgr.not(bfmgr.and(pChosenLiterals)),
                  pFmgr);

          if (remainingLiterals.isEmpty()) {
            return refinedInvariant;
          }

          pProver.pop(); // Pop the predecessor assertion
          pProver.pop(); // Pop the successor-violation assertion
          restored = false;
        }
      }

      return refinedInvariant;
    }

    private boolean determineRelevantLiterals(
        ProverEnvironmentWithFallback pProver,
        FormulaManagerView pFmgr,
        Collection<BooleanFormula> pInvariantAssertionComponents,
        Set<BooleanFormula> pRelevantLiterals)
        throws SolverException, InterruptedException {

      Set<BooleanFormula> literals =
          FluentIterable.from(pInvariantAssertionComponents)
              .transformAndConcat(
                  c -> SymbolicCandiateInvariant.getConjunctionOperands(pFmgr, c, true))
              .stream()
              .collect(Collectors.toCollection(HashSet::new));

      // We need to use all state literals
      Iterator<BooleanFormula> literalIterator = literals.iterator();
      while (literalIterator.hasNext()) {
        BooleanFormula literal = literalIterator.next();
        BooleanFormula uninstantiatedRemainingLiteral = pFmgr.uninstantiate(literal);
        if (pFmgr
            .extractVariableNames(uninstantiatedRemainingLiteral)
            .contains(TotalTransitionRelation.getLocationVariableName())) {
          pRelevantLiterals.add(uninstantiatedRemainingLiteral);
          literalIterator.remove();
        }
      }

      boolean isUnsat = pProver.isUnsat();

      // Push as many of the remaining invariant-violation assertions as required for
      // unsatisfiability
      literalIterator = literals.iterator();
      List<BooleanFormula> pushedLiterals = new ArrayList<>();
      while (!isUnsat && literalIterator.hasNext()) {
        BooleanFormula literal = literalIterator.next();
        pProver.push(literal);
        pushedLiterals.add(literal);
        literalIterator.remove();
        isUnsat = pProver.isUnsat();
      }
      int nPushedLiterals = pushedLiterals.size();

      if (!pProver.isUnsat()) {
        // Pop the invariant-violation assertions
        IntStream.range(0, nPushedLiterals)
            .forEach(
                i -> {
                  pProver.pop();
                });
        return false;
      }

      if (pProver.supportsUnsatCoreGeneration()) {
        pushedLiterals.retainAll(pProver.getUnsatCore());
      }
      for (BooleanFormula pushedLiteral : pushedLiterals) {
        pRelevantLiterals.add(pFmgr.uninstantiate(pushedLiteral));
      }

      // Pop the invariant-violation assertions
      IntStream.range(0, nPushedLiterals)
          .forEach(
              i -> {
                pProver.pop();
              });
      return true;
    }
  }

  public static <T extends CandidateInvariant>
      InvariantStrengthening<T, T> unsatCoreBasedStrengthening() {
    return new InvariantStrengthening<>() {

      @SuppressWarnings("unchecked")
      @Override
      public T strengthenInvariant(
          ProverEnvironmentWithFallback pProver,
          FormulaManagerView pFmgr,
          PredicateAbstractionManager pPam,
          T pInvariant,
          AssertCandidate pAssertPredecessor,
          AssertCandidate pAssertSuccessorViolation,
          AssertCandidate pAssertCti,
          Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
          Optional<BooleanFormula> pAssertedInvariants,
          NextCti pNextCti)
          throws SolverException, InterruptedException, CPATransferException {
        if (pInvariant instanceof SymbolicCandiateInvariant) {
          return (T)
              UnsatCoreBasedRefinement.INSTANCE.strengthenInvariant(
                  pProver,
                  pFmgr,
                  pPam,
                  (SymbolicCandiateInvariant) pInvariant,
                  pAssertPredecessor,
                  pAssertSuccessorViolation,
                  pAssertCti,
                  pStateViolationAssertions,
                  pAssertedInvariants,
                  pNextCti);
        }
        return pInvariant;
      }
    };
  }
}
