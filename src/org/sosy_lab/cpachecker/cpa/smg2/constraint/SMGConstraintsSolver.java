// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreatorUsingCConverter;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class SMGConstraintsSolver {
  private SMGConstraintsSolver.ConstraintsCache cache;
  private Solver solver;
  private ProverEnvironment prover;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;

  private CtoFormulaConverter converter;
  private SymbolicIdentifierLocator locator;

  /** Table of id constraints set, id identifier assignment, formula */
  private Map<Constraint, BooleanFormula> constraintFormulas = new HashMap<>();

  private BooleanFormula literalForSingleAssignment;

  private ConstraintsStatistics stats;

  private final SMGOptions options;

  public SMGConstraintsSolver(
      final Solver pSolver,
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pConverter,
      final ConstraintsStatistics pStats,
      SMGOptions pOptions) {
    solver = pSolver;
    formulaManager = pFormulaManager;
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    literalForSingleAssignment = booleanFormulaManager.makeVariable("__A");
    converter = pConverter;
    locator = SymbolicIdentifierLocator.getInstance();
    stats = pStats;
    options = pOptions;

    if (options.isUseConstraintCacheSubsets()) {
      cache = new SMGConstraintsSolver.MatchingConstraintsCache();
      if (options.isUseConstraintCacheSubsets()) {
        cache = new SMGConstraintsSolver.SubsetConstraintsCache(cache);
      }
      if (options.isUseConstraintCacheSupersets()) {
        cache = new SMGConstraintsSolver.SupersetConstraintsCache(cache);
      }
    } else {
      cache = new SMGConstraintsSolver.DummyCache();
    }
  }

  /**
   * Check pConstraintToCheck with all constraints in the given state, but does not add the
   * pConstraintToCheck to the state. Will add a model in SAT cases.
   *
   * @param stateForConstraints {@link SMGState}
   * @param pConstraintToCheck {@link Constraint} i.e. memory access bounds to check
   * @param pFunctionName {@link String} current stackframe function
   * @return true to UNSAT and unchanged state, false for SAT with model in the state, but not the
   *     constraint.
   * @throws UnrecognizedCodeException in case of unrecognized code in the constraints
   * @throws InterruptedException solver error on termination signal
   * @throws SolverException solver error
   */
  public BooleanAndSMGState isUnsat(
      SMGState stateForConstraints, Constraint pConstraintToCheck, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    BooleanAndSMGState result =
        isUnsat(stateForConstraints.addConstraint(pConstraintToCheck), pFunctionName);
    return BooleanAndSMGState.of(
        result.getBoolean(), result.getState().removeLastAddedConstraint());
  }

  // Used for trivial unsat checks that do not change the state/set a model in the state
  public boolean isUnsat(Constraint pConstraint, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    try {
      try {
        stats.timeForSolving.start();

        Boolean unsat = null; // assign null to fail fast if assignment is missed
        Set<Constraint> relevantConstraints = ImmutableSet.of(pConstraint);

        Collection<BooleanFormula> constraintsAsFormulas =
            getFullFormula(relevantConstraints, pFunctionName);
        SMGConstraintsSolver.CacheResult res = cache.getCachedResult(constraintsAsFormulas);

        if (res.isUnsat()) {
          unsat = true;

        } else if (res.isSat()) {
          unsat = false;

        } else {
          prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
          BooleanFormula singleConstraintFormula = booleanFormulaManager.and(constraintsAsFormulas);
          prover.push(singleConstraintFormula);

          try {
            stats.timeForSatCheck.start();
            unsat = prover.isUnsat();
          } finally {
            stats.timeForSatCheck.stop();
          }

          if (!unsat) {
            ImmutableList<ValueAssignment> newModelAsAssignment = prover.getModelAssignments();
            cache.addSat(constraintsAsFormulas, newModelAsAssignment);
          } else {
            cache.addUnsat(constraintsAsFormulas);
          }
        }

        return unsat;

      } finally {
        closeProver();
        stats.timeForSolving.stop();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns whether this state is unsatisfiable. A state without constraints (that is, an empty
   * state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public BooleanAndSMGState isUnsat(SMGState pConstraints, String pFunctionName)
      throws SolverException, InterruptedException, UnrecognizedCodeException {

    if (pConstraints.isEmptyConstraints()) {
      return BooleanAndSMGState.of(false, pConstraints);
    }
    try {
      try {
        stats.timeForSolving.start();

        Boolean unsat = null; // assign null to fail fast if assignment is missed
        Set<Constraint> relevantConstraints = getRelevantConstraints(pConstraints);

        Collection<BooleanFormula> constraintsAsFormulas =
            getFullFormula(relevantConstraints, pFunctionName);
        SMGConstraintsSolver.CacheResult res = cache.getCachedResult(constraintsAsFormulas);

        SMGState updatedState = pConstraints;
        if (res.isUnsat()) {
          unsat = true;

        } else if (res.isSat()) {
          unsat = false;
          updatedState = updatedState.copyAndSetModel(res.getModelAssignment());

        } else {
          prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
          BooleanFormula definitesAndConstraints =
              combineWithDefinites(constraintsAsFormulas, updatedState);
          prover.push(definitesAndConstraints);

          try {
            stats.timeForSatCheck.start();
            unsat = prover.isUnsat();
          } finally {
            stats.timeForSatCheck.stop();
          }

          if (!unsat) {
            ImmutableList<ValueAssignment> newModelAsAssignment = prover.getModelAssignments();
            updatedState = updatedState.copyAndSetModel(newModelAsAssignment);
            cache.addSat(constraintsAsFormulas, newModelAsAssignment);
            // doing this while the complete formula is still on the prover environment stack is
            // cheaper than performing another complete SAT check when the assignment is really
            // requested
            if (options.isResolveDefinites()) {
              updatedState =
                  updatedState.copyAndSetDefiniteAssignment(
                      resolveDefiniteAssignments(updatedState, newModelAsAssignment));
            }

            assert updatedState.getModel().containsAll(updatedState.getDefiniteAssignment())
                : "Model does not imply definites: "
                    + updatedState.getModel()
                    + " !=> "
                    + updatedState.getDefiniteAssignment();

          } else {
            try {
              assert prover.isUnsat()
                  : "Unsat with definite assignment, but not without. Definite assignment: "
                      + updatedState.getDefiniteAssignment();

            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            cache.addUnsat(constraintsAsFormulas);
          }
        }

        return BooleanAndSMGState.of(unsat, updatedState);

      } finally {
        closeProver();
        stats.timeForSolving.stop();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private BooleanFormula combineWithDefinites(
      Collection<BooleanFormula> pConstraintsAsFormulas, SMGState pConstraints) {

    BooleanFormula singleConstraintFormula = booleanFormulaManager.and(pConstraintsAsFormulas);
    BooleanFormula definites = getDefAssignmentsFormula(pConstraints);
    return booleanFormulaManager.and(definites, singleConstraintFormula);
  }

  private BooleanFormula getDefAssignmentsFormula(SMGState pConstraints) {
    return pConstraints.getDefiniteAssignment().stream()
        .map(ValueAssignment::getAssignmentAsFormula)
        .collect(booleanFormulaManager.toConjunction());
  }

  private BooleanFormula createLiteralLabel(BooleanFormula pLiteral, BooleanFormula pFormula) {
    return booleanFormulaManager.implication(pLiteral, pFormula);
  }

  private Set<Constraint> getRelevantConstraints(SMGState pConstraints) {
    Set<Constraint> relevantConstraints = new HashSet<>();
    if (options.isPerformMinimalConstraintSatCheck()
        && pConstraints.getLastAddedConstraint().isPresent()) {
      try {
        stats.timeForIndependentComputation.start();
        Constraint lastConstraint = pConstraints.getLastAddedConstraint().orElseThrow();
        // Always add the last added constraint to the set of relevant constraints.
        // It may not contain any symbolic identifiers (e.g., 0 == 5) and will thus
        // not be automatically included in the iteration over dependent sets below.
        relevantConstraints.add(lastConstraint);

        Set<Constraint> leftOverConstraints = new HashSet<>(pConstraints.getConstraints());
        Set<SymbolicIdentifier> newRelevantIdentifiers = lastConstraint.accept(locator);
        Set<SymbolicIdentifier> relevantIdentifiers;
        do {
          relevantIdentifiers = newRelevantIdentifiers;
          Iterator<Constraint> it = leftOverConstraints.iterator();
          while (it.hasNext()) {
            Constraint currentC = it.next();
            Set<SymbolicIdentifier> containedIdentifiers = currentC.accept(locator);
            if (!Sets.intersection(containedIdentifiers, relevantIdentifiers).isEmpty()) {
              newRelevantIdentifiers = Sets.union(newRelevantIdentifiers, containedIdentifiers);
              relevantConstraints.add(currentC);
              it.remove();
            }
          }
        } while (!newRelevantIdentifiers.equals(relevantIdentifiers));

      } finally {
        stats.timeForIndependentComputation.stop();
      }

    } else {
      relevantConstraints = new HashSet<>(pConstraints.getConstraints());
    }

    return relevantConstraints;
  }

  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  private ImmutableCollection<ValueAssignment> resolveDefiniteAssignments(
      SMGState pConstraints, List<ValueAssignment> pModel)
      throws InterruptedException, SolverException {
    try {
      stats.timeForDefinitesComputation.start();

      return computeDefiniteAssignment(pConstraints, pModel);

    } finally {
      stats.timeForDefinitesComputation.stop();
    }
  }

  private ImmutableCollection<ValueAssignment> computeDefiniteAssignment(
      SMGState pState, List<ValueAssignment> pModel) throws SolverException, InterruptedException {

    ImmutableCollection<ValueAssignment> existingDefinites = pState.getDefiniteAssignment();
    ImmutableSet.Builder<ValueAssignment> newDefinites = ImmutableSet.builder();

    for (ValueAssignment val : pModel) {
      if (SymbolicValues.isSymbolicTerm(val.getName())
          && (existingDefinites.contains(val) || isOnlySatisfyingAssignment(val))) {
        newDefinites.add(val);
      }
    }
    return newDefinites.build();
  }

  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm)
      throws SolverException, InterruptedException {

    BooleanFormula prohibitAssignment = formulaManager.makeNot(pTerm.getAssignmentAsFormula());

    prohibitAssignment = createLiteralLabel(literalForSingleAssignment, prohibitAssignment);
    prover.push(prohibitAssignment);
    try {
      boolean isUnsat =
          prover.isUnsatWithAssumptions(Collections.singleton(literalForSingleAssignment));
      prover.pop();

      return isUnsat;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private FormulaCreator getFormulaCreator(String pFunctionName) {
    return new FormulaCreatorUsingCConverter(converter, pFunctionName);
  }

  /**
   * Returns the set of formulas representing all constraints of this state. If no constraints
   * exist, this method will return an empty set.
   *
   * @return the set of formulas representing all constraints of this state
   * @throws UnrecognizedCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  private Collection<BooleanFormula> getFullFormula(
      Collection<Constraint> pConstraints, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {

    List<BooleanFormula> formulas = new ArrayList<>(pConstraints.size());
    for (Constraint c : pConstraints) {
      if (!constraintFormulas.containsKey(c)) {
        constraintFormulas.put(c, createConstraintFormulas(c, pFunctionName));
      }
      formulas.add(constraintFormulas.get(c));
    }

    return formulas;
  }

  private BooleanFormula createConstraintFormulas(Constraint pConstraint, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {
    assert !constraintFormulas.containsKey(pConstraint)
        : "Trying to add a formula that already exists!";

    return getFormulaCreator(pFunctionName).createFormula(pConstraint);
  }

  private interface ConstraintsCache {
    SMGConstraintsSolver.CacheResult getCachedResult(Collection<BooleanFormula> pConstraints);

    void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment);

    void addUnsat(Collection<BooleanFormula> pConstraints);
  }

  private final class MatchingConstraintsCache implements SMGConstraintsSolver.ConstraintsCache {

    // TODO This should use an immutable data structure as key, and not Collection but List/Set
    private Map<Collection<BooleanFormula>, SMGConstraintsSolver.CacheResult> cacheMap =
        new HashMap<>();

    @Override
    @SuppressWarnings("CollectionUndefinedEquality") // TODO there is a bug here
    public SMGConstraintsSolver.CacheResult getCachedResult(
        Collection<BooleanFormula> pConstraints) {
      stats.cacheLookups.inc();
      stats.directCacheLookupTime.start();
      try {
        if (cacheMap.containsKey(pConstraints)) {
          stats.directCacheHits.inc();
          return cacheMap.get(pConstraints);

        } else {
          return SMGConstraintsSolver.CacheResult.getUnknown();
        }
      } finally {
        stats.directCacheLookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints, SMGConstraintsSolver.CacheResult.getSat(pModelAssignment));
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints, SMGConstraintsSolver.CacheResult.getUnsat());
    }

    private void add(
        Collection<BooleanFormula> pConstraints, SMGConstraintsSolver.CacheResult pResult) {
      cacheMap.put(pConstraints, pResult);
    }
  }

  private final class SupersetConstraintsCache implements SMGConstraintsSolver.ConstraintsCache {

    private SMGConstraintsSolver.ConstraintsCache delegate;

    /** Multimap that maps each constraint to all sets of constraints that it occurs in */
    private Multimap<BooleanFormula, Set<BooleanFormula>> constraintContainedIn =
        HashMultimap.create();

    public SupersetConstraintsCache(final SMGConstraintsSolver.ConstraintsCache pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public SMGConstraintsSolver.CacheResult getCachedResult(
        Collection<BooleanFormula> pConstraints) {
      SMGConstraintsSolver.CacheResult res = delegate.getCachedResult(pConstraints);
      if (!res.isSat() && !res.isUnsat()) {
        try {
          stats.supersetLookupTime.start();
          res = getCachedResultOfSuperset(pConstraints);
          if (res.isSat() || res.isUnsat()) {
            stats.supersetCacheHits.inc();
          }
        } finally {
          stats.supersetLookupTime.stop();
        }
      }
      return res;
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints);
      delegate.addSat(pConstraints, pModelAssignment);
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints);
      delegate.addUnsat(pConstraints);
    }

    private void add(Collection<BooleanFormula> pConstraints) {
      for (BooleanFormula c : pConstraints) {
        constraintContainedIn.put(c, ImmutableSet.copyOf(pConstraints));
      }
    }

    SMGConstraintsSolver.CacheResult getCachedResultOfSuperset(
        Collection<BooleanFormula> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Set<BooleanFormula>> containAllConstraints = null;
      for (BooleanFormula c : pConstraints) {
        Set<Set<BooleanFormula>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
        if (containAllConstraints == null) {
          containAllConstraints = containC;
        } else {
          containAllConstraints = Sets.intersection(containAllConstraints, containC);
        }

        if (containAllConstraints.isEmpty()) {
          return SMGConstraintsSolver.CacheResult.getUnknown();
        }
      }

      checkNotNull(containAllConstraints);
      int sizeOfQuery = pConstraints.size();
      for (Set<BooleanFormula> col : containAllConstraints) {
        SMGConstraintsSolver.CacheResult cachedResult = delegate.getCachedResult(col);
        if (sizeOfQuery <= col.size() && cachedResult.isSat()) {
          // currently considered collection is a superset of the queried collection
          return cachedResult;
        }
      }
      return SMGConstraintsSolver.CacheResult.getUnknown();
    }
  }

  private final class SubsetConstraintsCache implements SMGConstraintsSolver.ConstraintsCache {

    private SMGConstraintsSolver.ConstraintsCache delegate;

    /** Multimap that maps each constraint to all sets of constraints that it occurred in */
    private Multimap<BooleanFormula, Set<BooleanFormula>> constraintContainedIn =
        HashMultimap.create();

    public SubsetConstraintsCache(final SMGConstraintsSolver.ConstraintsCache pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public SMGConstraintsSolver.CacheResult getCachedResult(
        Collection<BooleanFormula> pConstraints) {
      SMGConstraintsSolver.CacheResult res = delegate.getCachedResult(pConstraints);
      if (!res.isSat() && !res.isUnsat()) {
        try {
          stats.subsetLookupTime.start();
          res = getCachedResultOfSubset(pConstraints);
          if (res.isSat() || res.isUnsat()) {
            stats.subsetCacheHits.inc();
          }
        } finally {
          stats.subsetLookupTime.stop();
        }
      }
      return res;
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints);
      delegate.addSat(pConstraints, pModelAssignment);
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints);
      delegate.addUnsat(pConstraints);
    }

    private void add(Collection<BooleanFormula> pConstraints) {
      for (BooleanFormula c : pConstraints) {
        constraintContainedIn.put(c, ImmutableSet.copyOf(pConstraints));
      }
    }

    SMGConstraintsSolver.CacheResult getCachedResultOfSubset(
        Collection<BooleanFormula> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Set<BooleanFormula>> containAllConstraints = new HashSet<>();
      for (BooleanFormula c : pConstraints) {
        Set<Set<BooleanFormula>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
        containAllConstraints.addAll(containC);
      }

      int sizeOfQuery = pConstraints.size();
      for (Set<BooleanFormula> col : containAllConstraints) {
        SMGConstraintsSolver.CacheResult cachedResult = delegate.getCachedResult(col);
        if (sizeOfQuery >= col.size() && cachedResult.isUnsat()) {
          // currently considered collection is a subset of the queried collection
          return cachedResult;
        }
      }
      return SMGConstraintsSolver.CacheResult.getUnknown();
    }
  }

  private static final class DummyCache implements SMGConstraintsSolver.ConstraintsCache {

    @Override
    public SMGConstraintsSolver.CacheResult getCachedResult(
        Collection<BooleanFormula> pConstraints) {
      return SMGConstraintsSolver.CacheResult.getUnknown();
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      // do nothing
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      // do nothing
    }
  }

  private static class CacheResult {
    enum Result {
      SAT,
      UNSAT,
      UNKNOWN
    }

    private static final SMGConstraintsSolver.CacheResult UNSAT_SINGLETON =
        new SMGConstraintsSolver.CacheResult(
            SMGConstraintsSolver.CacheResult.Result.UNSAT, Optional.empty());
    private static final SMGConstraintsSolver.CacheResult UNKNOWN_SINGLETON =
        new SMGConstraintsSolver.CacheResult(
            SMGConstraintsSolver.CacheResult.Result.UNKNOWN, Optional.empty());

    private SMGConstraintsSolver.CacheResult.Result result;
    private Optional<ImmutableList<ValueAssignment>> modelAssignment;

    public static SMGConstraintsSolver.CacheResult getSat(
        ImmutableList<ValueAssignment> pModelAssignment) {
      return new SMGConstraintsSolver.CacheResult(
          SMGConstraintsSolver.CacheResult.Result.SAT, Optional.of(pModelAssignment));
    }

    public static SMGConstraintsSolver.CacheResult getUnsat() {
      return UNSAT_SINGLETON;
    }

    public static SMGConstraintsSolver.CacheResult getUnknown() {
      return UNKNOWN_SINGLETON;
    }

    private CacheResult(
        SMGConstraintsSolver.CacheResult.Result pResult,
        Optional<ImmutableList<ValueAssignment>> pModelAssignment) {
      result = pResult;
      modelAssignment = pModelAssignment;
    }

    public boolean isSat() {
      return result.equals(SMGConstraintsSolver.CacheResult.Result.SAT);
    }

    public boolean isUnsat() {
      return result.equals(SMGConstraintsSolver.CacheResult.Result.UNSAT);
    }

    public ImmutableList<ValueAssignment> getModelAssignment() {
      checkState(modelAssignment.isPresent(), "No model exists");
      return modelAssignment.orElseThrow();
    }
  }
}
