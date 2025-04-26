// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreatorUsingCConverter;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult.Satisfiability;
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

@Options(prefix = "cpa.constraints")
public class ConstraintsSolver {

  /**
   * c Result of a single constraint solving.
   *
   * @param checkedConstraints the constraints checked for satisfiability. This may be only the
   *     relevant subset of the constraints provided to the solver.
   * @param satisfiability the satisfiability result of the checked constraints
   * @param model the model of the checked constraints, if satisfiable.
   * @param definiteAssignments the definite assignments in the model, if satisfiable.
   */
  public record SolverResult(
      ImmutableSet<Constraint> checkedConstraints,
      Satisfiability satisfiability,
      Optional<ImmutableList<ValueAssignment>> model,
      Optional<ImmutableCollection<ValueAssignment>> definiteAssignments) {
    public enum Satisfiability {
      SAT,
      UNSAT
    }

    public boolean isSAT() {
      return satisfiability.equals(Satisfiability.SAT);
    }

    public boolean isUNSAT() {
      return satisfiability.equals(Satisfiability.UNSAT);
    }
  }

  @Option(secure = true, description = "Whether to use subset caching", name = "cacheSubsets")
  private boolean cacheSubsets = false;

  @Option(secure = true, description = "Whether to use superset caching", name = "cacheSupersets")
  private boolean cacheSupersets = false;

  @Option(
      secure = true,
      description = "Whether to perform SAT checks only for the last added constraint",
      name = "minimalSatCheck")
  private boolean performMinimalSatCheck = false;

  @Option(
      secure = true,
      description = "Whether to perform caching of constraint satisfiability results",
      name = "cache")
  private boolean doCaching = true;

  @Option(
      secure = true,
      description =
          "Resolve definite assignments. Note: Currently not working properly. Might result in"
              + " inefficient SMT solver usage with freshProverForEachSATCheck = false.",
      name = "resolveDefinites")
  private boolean resolveDefinites = false;

  @Option(
      secure = true,
      description =
          "Whether to create a new, fresh prover for each SAT check with an SMT solver or, if"
              + " false, try to reuse the prover stack as far as possible (may be helpful when"
              + " formulas are built on top of each other often).",
      name = "freshProverForEachSATCheck")
  private boolean freshProverForEachSATCheck = false;

  private ConstraintsCache cache;
  private Solver solver;
  private ProverEnvironment persistentProver;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;

  private CtoFormulaConverter converter;
  private SymbolicIdentifierLocator locator;

  /** Table of id constraints set, id identifier assignment, formula */
  private Map<Constraint, BooleanFormula> constraintFormulas = new HashMap<>();

  private BooleanFormula literalForSingleAssignment;

  private ConstraintsStatistics stats;

  private Deque<BooleanFormula> currentConstraintsOnProver = new ArrayDeque<>();

  public ConstraintsSolver(
      final Configuration pConfig,
      final Solver pSolver,
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pConverter,
      final ConstraintsStatistics pStats)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    solver = pSolver;
    formulaManager = pFormulaManager;
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    literalForSingleAssignment = booleanFormulaManager.makeVariable("__A");
    converter = pConverter;
    locator = SymbolicIdentifierLocator.getInstance();
    stats = pStats;
    persistentProver = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);

    if (doCaching) {
      cache = new MatchingConstraintsCache();
      if (cacheSubsets) {
        cache = new SubsetConstraintsCache(cache);
      }
      if (cacheSupersets) {
        cache = new SupersetConstraintsCache(cache);
      }
    } else {
      cache = new DummyCache();
    }
  }

  /**
   * Returns whether the given constraint is unsatisfiable. A state without constraints (that is, an
   * empty state), is always satisfiable. Will try to reuse the existing provers stack as far as
   * possible if useFreshDistinctProver=false.
   *
   * @param pConstraintToCheck the constraint to check
   * @param pFunctionName the name of this constraints function scope
   * @param forceFreshDistinctProver if true, uses a fresh but distinct prover that is closed after
   *     the method is finished. If false, will reuse the exising prover and tries to reuse the
   *     current solver stack as far as possible. This might also lose cached information in the
   *     solver (making it slower) if the old constraints are not a subset of the new constraints.
   * @return <code>true</code> if this constraint is unsatisfiable, <code>false</code> otherwise
   */
  public Satisfiability checkUnsat(
      Constraint pConstraintToCheck, String pFunctionName, boolean forceFreshDistinctProver)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    ConstraintsState s = new ConstraintsState(Collections.singleton(pConstraintToCheck));
    return checkUnsat(s, pFunctionName, forceFreshDistinctProver).satisfiability();
  }

  /**
   * Returns whether this state is unsatisfiable. A state without constraints (that is, an empty
   * state), is always satisfiable. Will try to reuse the existing provers stack as far as possible
   * if useFreshDistinctProver=false.
   *
   * @param pConstraintToCheck the constraint to check
   * @param pFunctionName the name of this constraints function scope
   * @param forceFreshDistinctProver if true, uses a fresh but distinct prover that is closed after
   *     the method is finished. If false, will reuse the exising prover and tries to reuse the
   *     current solver stack as far as possible. This might also lose cached information in the
   *     solver (making it slower) if the old constraints are not a subset of the new constraints.
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public SolverResult checkUnsat(
      ConstraintsState pConstraintToCheck, String pFunctionName, boolean forceFreshDistinctProver)
      throws SolverException, InterruptedException, UnrecognizedCodeException {

    if (freshProverForEachSATCheck) {
      forceFreshDistinctProver = true;
    }

    if (pConstraintToCheck.isEmpty()) {
      return new SolverResult(
          ImmutableSet.of(),
          Satisfiability.SAT,
          Optional.of(ImmutableList.of()),
          Optional.of(ImmutableList.of()));
    }

    ProverEnvironment prover = persistentProver;

    try {
      stats.timeForSolving.start();

      boolean unsat;
      ImmutableSet<Constraint> relevantConstraints = getRelevantConstraints(pConstraintToCheck);

      Collection<BooleanFormula> constraintsAsFormulas =
          getFullFormula(relevantConstraints, pFunctionName);
      CacheResult res = cache.getCachedResult(constraintsAsFormulas);

      ImmutableList<ValueAssignment> satisfyingModel = null;
      ImmutableCollection<ValueAssignment> definiteAssignmentsInModel = null;
      if (res.isUnsat()) {
        unsat = true;

      } else if (res.isSat()) {
        unsat = false;
        satisfyingModel = res.getModelAssignment();

      } else {
        try {
          stats.timeForProverPreparation.start();
          if (forceFreshDistinctProver) {
            stats.distinctFreshProversUsed.inc();
            prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
            BooleanFormula definitesAndConstraints =
                combineWithDefinites(constraintsAsFormulas, pConstraintToCheck);
            prover.push(definitesAndConstraints);

          } else {
            stats.persistentProverUsed.inc();
            // def assignments are automatically applied by the persistentProver
            preparePersistentProverForCheck(constraintsAsFormulas);
          }
        } finally {
          stats.timeForProverPreparation.stop();
        }

        try {
          stats.timeForSatCheck.start();
          unsat = prover.isUnsat();
        } finally {
          stats.timeForSatCheck.stop();
        }

        if (!unsat) {
          satisfyingModel = prover.getModelAssignments();
          cache.addSat(constraintsAsFormulas, satisfyingModel);
          // doing this while the complete formula is still on the prover environment stack is
          // cheaper than performing another complete SAT check when the assignment is really
          // requested
          if (resolveDefinites) {
            definiteAssignmentsInModel =
                resolveDefiniteAssignments(pConstraintToCheck, satisfyingModel, prover);
            assert satisfyingModel.containsAll(definiteAssignmentsInModel)
                : "Model does not imply definites: "
                    + satisfyingModel
                    + " !=> "
                    + definiteAssignmentsInModel;
          }

        } else {
          cache.addUnsat(constraintsAsFormulas);
        }
      }

      // TODO:
      // pop definite assignments. this can only happen after the model produced by the unsat-check
      // is not used anymore
      // prover.pop();

      return new SolverResult(
          relevantConstraints,
          unsat ? Satisfiability.UNSAT : Satisfiability.SAT,
          Optional.ofNullable(satisfyingModel),
          Optional.ofNullable(definiteAssignmentsInModel));

    } finally {
      stats.timeForSolving.stop();
      if (forceFreshDistinctProver) {
        checkState(prover != persistentProver);
        prover.close();
      }
    }
  }

  private void preparePersistentProverForCheck(Collection<BooleanFormula> constraintsToCheck)
      throws InterruptedException {
    int totalKept = 0;
    int totalRemoved = 0;
    Set<BooleanFormula> retainedFormulas = new HashSet<>();
    Set<BooleanFormula> formulasToRemove = new HashSet<>();

    // This iterator goes from the top of the stack to the bottom
    Iterator<BooleanFormula> currentStack = currentConstraintsOnProver.descendingIterator();
    // Descending iterator through current stack
    //   If stackFormula is not in constraintsToCheck and retained empty, remove from top
    //   eif stackformula is not in constraintsToCheck and retained not empty,
    //     remember all retained from stack as we need to pop them and set retained == empty
    //   eIf stackFormula is in constraintsToCheck, don't remove from stack,
    //     add to retained
    // End iterator
    // Remove remembered retained levels that should be popped from stack
    // At the end, add constraints missing to the stack in order
    while (currentStack.hasNext()) {
      BooleanFormula constraintOnStack = currentStack.next();

      if (constraintsToCheck.contains(constraintOnStack)) {
        totalKept++;
        retainedFormulas.add(constraintOnStack);
      } else {
        totalRemoved++;
        currentStack.remove();
        // Potentially problematic on level 1 for some solvers!
        persistentProver.pop();
        if (!retainedFormulas.isEmpty()) {
          // We need to remove levels that we already iterated through, they are found in
          // retainedFormulas
          formulasToRemove.addAll(retainedFormulas);
          for (int i = 0; i < retainedFormulas.size(); i++) {
            persistentProver.pop();
            totalRemoved++;
          }
          retainedFormulas = new HashSet<>();
        }
      }
    }

    if (totalKept + totalRemoved > 0) {
      stats.reuseRatio.setNextValue((double) totalKept / (totalKept + totalRemoved));
    }

    if (totalRemoved == 0) {
      // We know that we had no cache hit, so it is a constraint combination that we know -> there
      // is new constraints
      stats.persistentProverUsedIncrementallyPushedWithoutPop.inc();
    }

    if (formulasToRemove.isEmpty()) {
      if (totalRemoved != 0) {
        stats.persistentProverUsedIncrementallyFormulasPopdAndNotRepushed.inc();
      }
    } else {
      stats.persistentProverUsedIncrementallyFormulasPopdAndRepushed.inc();
      currentConstraintsOnProver.removeAll(formulasToRemove);
    }

    for (BooleanFormula f : constraintsToCheck) {
      if (!currentConstraintsOnProver.contains(f)) {
        currentConstraintsOnProver.push(f);
        persistentProver.push(f);
      }
    }
  }

  private BooleanFormula combineWithDefinites(
      Collection<BooleanFormula> pConstraintsAsFormulas, ConstraintsState pConstraints) {

    BooleanFormula singleConstraintFormula = booleanFormulaManager.and(pConstraintsAsFormulas);
    BooleanFormula definites = getDefAssignmentsFormula(pConstraints);
    return booleanFormulaManager.and(definites, singleConstraintFormula);
  }

  private BooleanFormula getDefAssignmentsFormula(ConstraintsState pConstraints) {
    return pConstraints.getDefiniteAssignment().stream()
        .map(ValueAssignment::getAssignmentAsFormula)
        .collect(booleanFormulaManager.toConjunction());
  }

  private BooleanFormula createLiteralLabel(BooleanFormula pLiteral, BooleanFormula pFormula) {
    return booleanFormulaManager.implication(pLiteral, pFormula);
  }

  private ImmutableSet<Constraint> getRelevantConstraints(ConstraintsState pConstraints) {
    ImmutableSet.Builder<Constraint> relevantConstraints = ImmutableSet.builder();
    if (performMinimalSatCheck && pConstraints.getLastAddedConstraint().isPresent()) {
      try {
        stats.timeForIndependentComputation.start();
        Constraint lastConstraint = pConstraints.getLastAddedConstraint().orElseThrow();
        // Always add the last added constraint to the set of relevant constraints.
        // It may not contain any symbolic identifiers (e.g., 0 == 5) and will thus
        // not be automatically included in the iteration over dependent sets below.
        relevantConstraints.add(lastConstraint);

        Set<Constraint> leftOverConstraints = new HashSet<>(pConstraints);
        leftOverConstraints.remove(lastConstraint);
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
      return ImmutableSet.copyOf(pConstraints);
    }

    return relevantConstraints.build();
  }

  private ImmutableCollection<ValueAssignment> resolveDefiniteAssignments(
      ConstraintsState pConstraints, List<ValueAssignment> pModel, ProverEnvironment prover)
      throws InterruptedException, SolverException {
    try {
      stats.timeForDefinitesComputation.start();

      return computeDefiniteAssignment(pConstraints, pModel, prover);

    } finally {
      stats.timeForDefinitesComputation.stop();
    }
  }

  private ImmutableCollection<ValueAssignment> computeDefiniteAssignment(
      ConstraintsState pState, List<ValueAssignment> pModel, ProverEnvironment prover)
      throws SolverException, InterruptedException {

    ImmutableCollection<ValueAssignment> existingDefinites = pState.getDefiniteAssignment();
    ImmutableSet.Builder<ValueAssignment> newDefinites = ImmutableSet.builder();

    for (ValueAssignment val : pModel) {
      if (SymbolicValues.isSymbolicTerm(val.getName())
          && (existingDefinites.contains(val) || isOnlySatisfyingAssignment(val, prover))) {
        stats.definiteAssignmentsFound.inc();
        newDefinites.add(val);
      }
    }
    return newDefinites.build();
  }

  // TODO: use distinct prover? Is more inefficient on this check, but this check might make the
  //  other checks more inefficient for persistent provers.
  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm, ProverEnvironment prover)
      throws SolverException, InterruptedException {

    BooleanFormula prohibitAssignment = formulaManager.makeNot(pTerm.getAssignmentAsFormula());

    prohibitAssignment = createLiteralLabel(literalForSingleAssignment, prohibitAssignment);
    prover.push(prohibitAssignment);
    boolean isUnsat =
        prover.isUnsatWithAssumptions(Collections.singleton(literalForSingleAssignment));
    prover.pop();

    return isUnsat;
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
    CacheResult getCachedResult(Collection<BooleanFormula> pConstraints);

    void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment);

    void addUnsat(Collection<BooleanFormula> pConstraints);
  }

  private final class MatchingConstraintsCache implements ConstraintsCache {

    private Map<ImmutableSet<BooleanFormula>, CacheResult> cacheMap = new HashMap<>();

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      ImmutableSet<BooleanFormula> cacheKey = ImmutableSet.copyOf(pConstraints);
      stats.cacheLookups.inc();
      stats.directCacheLookupTime.start();
      try {
        if (cacheMap.containsKey(cacheKey)) {
          stats.directCacheHits.inc();
          return cacheMap.get(cacheKey);

        } else {
          return CacheResult.getUnknown();
        }
      } finally {
        stats.directCacheLookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints, CacheResult.getSat(pModelAssignment));
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints, CacheResult.getUnsat());
    }

    private void add(Collection<BooleanFormula> pConstraints, CacheResult pResult) {
      cacheMap.put(ImmutableSet.copyOf(pConstraints), pResult);
    }
  }

  private final class SupersetConstraintsCache implements ConstraintsCache {

    private ConstraintsCache delegate;

    /** Multimap that maps each constraint to all sets of constraints that it occurs in */
    private Multimap<BooleanFormula, Set<BooleanFormula>> constraintContainedIn =
        HashMultimap.create();

    public SupersetConstraintsCache(final ConstraintsCache pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      CacheResult res = delegate.getCachedResult(pConstraints);
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

    CacheResult getCachedResultOfSuperset(Collection<BooleanFormula> pConstraints) {
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
          return CacheResult.getUnknown();
        }
      }

      checkNotNull(containAllConstraints);
      int sizeOfQuery = pConstraints.size();
      for (Set<BooleanFormula> col : containAllConstraints) {
        CacheResult cachedResult = delegate.getCachedResult(col);
        if (sizeOfQuery <= col.size() && cachedResult.isSat()) {
          // currently considered collection is a superset of the queried collection
          return cachedResult;
        }
      }
      return CacheResult.getUnknown();
    }
  }

  private final class SubsetConstraintsCache implements ConstraintsCache {

    private ConstraintsCache delegate;

    /** Multimap that maps each constraint to all sets of constraints that it occurred in */
    private Multimap<BooleanFormula, Set<BooleanFormula>> constraintContainedIn =
        HashMultimap.create();

    public SubsetConstraintsCache(final ConstraintsCache pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      CacheResult res = delegate.getCachedResult(pConstraints);
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

    CacheResult getCachedResultOfSubset(Collection<BooleanFormula> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Set<BooleanFormula>> containAllConstraints = new HashSet<>();
      for (BooleanFormula c : pConstraints) {
        Set<Set<BooleanFormula>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
        containAllConstraints.addAll(containC);
      }

      int sizeOfQuery = pConstraints.size();
      for (Set<BooleanFormula> col : containAllConstraints) {
        CacheResult cachedResult = delegate.getCachedResult(col);
        if (sizeOfQuery >= col.size() && cachedResult.isUnsat()) {
          // currently considered collection is a subset of the queried collection
          return cachedResult;
        }
      }
      return CacheResult.getUnknown();
    }
  }

  private static final class DummyCache implements ConstraintsCache {

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      return CacheResult.getUnknown();
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

    private static final CacheResult UNSAT_SINGLETON =
        new CacheResult(Result.UNSAT, Optional.empty());
    private static final CacheResult UNKNOWN_SINGLETON =
        new CacheResult(Result.UNKNOWN, Optional.empty());

    private Result result;
    private Optional<ImmutableList<ValueAssignment>> modelAssignment;

    public static CacheResult getSat(ImmutableList<ValueAssignment> pModelAssignment) {
      return new CacheResult(Result.SAT, Optional.of(pModelAssignment));
    }

    public static CacheResult getUnsat() {
      return UNSAT_SINGLETON;
    }

    public static CacheResult getUnknown() {
      return UNKNOWN_SINGLETON;
    }

    private CacheResult(Result pResult, Optional<ImmutableList<ValueAssignment>> pModelAssignment) {
      result = pResult;
      modelAssignment = pModelAssignment;
    }

    public boolean isSat() {
      return result.equals(Result.SAT);
    }

    public boolean isUnsat() {
      return result.equals(Result.UNSAT);
    }

    public ImmutableList<ValueAssignment> getModelAssignment() {
      checkState(modelAssignment.isPresent(), "No model exists");
      return modelAssignment.orElseThrow();
    }
  }
}
