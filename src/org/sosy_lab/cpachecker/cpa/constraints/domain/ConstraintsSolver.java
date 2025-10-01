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
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
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
      description =
          "Whether to perform SAT checks only for the last added constraint, as well as all"
              + " constraints dependent on it. Might improve solver performance.",
      name = "minimalSatCheck")
  private boolean performMinimalSatCheck = true;

  @Option(
      secure = true,
      description = "Whether to perform caching of constraint satisfiability results",
      name = "cache")
  private boolean doCaching = true;

  @Option(
      secure = true,
      description =
          "Resolve definite assignments, i.e. use the solver to find out whether there is only a"
              + " single satisfiable value for variables and assign it. Might result in inefficient"
              + " SMT solver usage with reuseSolver = true.",
      name = "resolveDefinites")
  private boolean resolveDefinites = false;

  @Option(
      secure = true,
      description =
          "Whether to create a new, fresh solver instance for each SAT check with an SMT solver "
              + "(=false), or try to reuse the solver and its previous constraints and results as "
              + "far as possible (may be helpful/faster when formulas are built on top of each "
              + "other often).",
      name = "reuseSolver")
  private boolean reuseSolver = true;

  private final ConstraintsCache cache;
  private final Solver solver;
  private final ProverEnvironment persistentProver;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView booleanFormulaManager;

  private final CtoFormulaConverter converter;
  private final SymbolicIdentifierLocator locator;

  /** Table of id constraints set, id identifier assignment, formula */
  private final Map<Constraint, BooleanFormula> constraintFormulas = new HashMap<>();

  private final BooleanFormula literalForSingleAssignment;

  private final ConstraintsStatistics stats;

  private final Deque<BooleanFormula> currentConstraintsOnProver = new ArrayDeque<>();

  private final MachineModel machineModel;

  public FormulaManagerView getFormulaManager() {
    return formulaManager;
  }

  public ConstraintsSolver(
      final Configuration pConfig,
      final MachineModel pMachineModel,
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
    machineModel = pMachineModel;

    ConstraintsCache cacheBuild;
    if (doCaching) {
      cacheBuild = new MatchingConstraintsCache();
      if (cacheSubsets) {
        cacheBuild = new SubsetConstraintsCache(cacheBuild);
      }
      if (cacheSupersets) {
        cacheBuild = new SupersetConstraintsCache(cacheBuild);
      }
    } else {
      cacheBuild = new DummyCache();
    }
    cache = cacheBuild;
  }

  /**
   * Returns the resulting {@link Satisfiability} for checking the given {@link Constraint}. Will
   * build a new, empty SMT solver instance (i.e. {@link ProverEnvironment}) to calculate results.
   * Only to be used for irrelevant models in SAT cases.
   *
   * @param pSingleConstraintToCheck the single {@link Constraint} to check.
   * @param pFunctionName the name of the function scope of pSingleConstraintToCheck.
   * @return {@link Satisfiability} of the given constraint.
   */
  public Satisfiability checkUnsatWithFreshSolver(
      Constraint pSingleConstraintToCheck, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    ConstraintsState s = new ConstraintsState(ImmutableSet.of(pSingleConstraintToCheck));
    return checkUnsatWithFreshSolver(s, pFunctionName).satisfiability();
  }

  /**
   * Returns the resulting {@link Satisfiability} for checking the given {@link Constraint}. Will
   * try to reuse the existing {@link ProverEnvironment} incrementally, as far as possible, if
   * option {@link #reuseSolver} is true. Incremental solving can improve computation time by
   * re-using information stored in the solver from previous computations. This effect is strongest
   * when the previously checked constraints are a true subset of the constraints in {@code
   * pConstraintsToCheck}. More information about solver reuse can be found in the description of
   * {@link #checkUnsat}. If option {@link #reuseSolver} is false, this method behaves like {@link
   * #checkUnsatWithFreshSolver(Constraint, String)}.
   *
   * @param pSingleConstraintToCheck the single {@link Constraint} to check.
   * @param pFunctionName the name of the function scope of pSingleConstraintToCheck.
   * @return {@link Satisfiability} of the given constraint.
   */
  public Satisfiability checkUnsatWithOptionDefinedSolverReuse(
      Constraint pSingleConstraintToCheck, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    ConstraintsState s = new ConstraintsState(Collections.singleton(pSingleConstraintToCheck));
    return checkUnsatWithOptionDefinedSolverReuse(s, pFunctionName).satisfiability();
  }

  /**
   * Returns the given constraints {@link Satisfiability} within a {@link SolverResult}, bundling
   * any satisfying model automatically for {@link Satisfiability#SAT} results. A state without
   * constraints (that is, an empty state), is always {@link Satisfiability#SAT}. Will build a new,
   * empty SMT solver instance (i.e. {@link ProverEnvironment}) to calculate results.
   *
   * @param pConstraintsToCheck the single constraint to check.
   * @param pFunctionName the name of the function scope of {@code pConstraintsToCheck}.
   * @return {@link SolverResult} with the {@link Satisfiability} wrapped inside. The satisfying
   *     model is automatically included for {@link Satisfiability#SAT}.
   */
  public SolverResult checkUnsatWithFreshSolver(
      ConstraintsState pConstraintsToCheck, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    return checkUnsat(pConstraintsToCheck, pFunctionName, true);
  }

  /**
   * Returns the given constraints {@link Satisfiability} within a {@link SolverResult}, bundling
   * any satisfying model automatically for {@link Satisfiability#SAT} results. A state without
   * constraints (that is, an empty state), is always {@link Satisfiability#SAT}. Will try to reuse
   * the existing {@link ProverEnvironment} incrementally, as far as possible, if option {@link
   * #reuseSolver} is true. Incremental solving can improve computation time by re-using information
   * stored in the solver from previous computations. This effect is strongest when the previously
   * checked constraints are a true subset of the constraints in {@code pConstraintsToCheck}. More
   * information about incremental usage can be found in the description of {@link #checkUnsat}. If
   * option {@link #reuseSolver} is false, this method behaves like {@link
   * #checkUnsatWithFreshSolver(ConstraintsState, String)}.
   *
   * @param pConstraintsToCheck the constraints to check.
   * @param pFunctionName the name of the function scope of {@code pConstraintsToCheck}.
   * @return {@link SolverResult} with the {@link Satisfiability} wrapped inside. The satisfying
   *     model is automatically included for {@link Satisfiability#SAT}.
   */
  public SolverResult checkUnsatWithOptionDefinedSolverReuse(
      ConstraintsState pConstraintsToCheck, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    return checkUnsat(pConstraintsToCheck, pFunctionName, !reuseSolver);
  }

  /**
   * Returns the given constraints {@link Satisfiability} within a {@link SolverResult}, bundling
   * any satisfying model automatically for {@link Satisfiability#SAT} results. A state without
   * constraints (that is, an empty state), is always {@link Satisfiability#SAT}. Will try to reuse
   * the existing {@link ProverEnvironment} incrementally as far as possible. If parameter {@code
   * forceFreshProver} is false and option {@link #reuseSolver} is true.
   *
   * @param pConstraintsToCheck the constraints to check.
   * @param pFunctionName the name of the function scope of {@code pConstraintsToCheck}.
   * @param forceFreshProver if true, uses a new {@link ProverEnvironment} for checking the
   *     constraints, that is closed after the method is finished. If false, will use incremental
   *     solving, which can improve computation time by re-using information stored in the solver
   *     from previous computations. This effect is strongest when the previously checked
   *     constraints are a subset of the constraints in {@code pConstraintsToCheck}.
   * @return {@link SolverResult} with the {@link Satisfiability} wrapped inside. The satisfying
   *     model is automatically included for {@link Satisfiability#SAT}.
   */
  private SolverResult checkUnsat(
      ConstraintsState pConstraintsToCheck, String pFunctionName, boolean forceFreshProver)
      throws SolverException, InterruptedException, UnrecognizedCodeException {

    if (pConstraintsToCheck.isEmpty()) {
      return new SolverResult(
          ImmutableSet.of(),
          Satisfiability.SAT,
          Optional.of(ImmutableList.of()),
          Optional.of(ImmutableList.of()));
    }

    try {
      stats.timeForSolving.start();

      ImmutableSet<Constraint> relevantConstraints = getRelevantConstraints(pConstraintsToCheck);

      // This list is deduplicated due to the input-set
      ImmutableSet<BooleanFormula> constraintsAsFormulas =
          getFullFormula(relevantConstraints, pFunctionName);
      CacheResult res = cache.getCachedResult(constraintsAsFormulas);

      if (res.isUnsat()) {
        return new SolverResult(
            relevantConstraints, Satisfiability.UNSAT, Optional.empty(), Optional.empty());

      } else if (res.isSat()) {
        return new SolverResult(
            relevantConstraints, Satisfiability.SAT, res.getModelAssignment(), Optional.empty());

      } else {

        stats.timeForProverPreparation.start();
        if (forceFreshProver) {
          // Non-Incremental
          stats.distinctFreshProversUsed.inc();
          try (ProverEnvironment prover =
              solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            BooleanFormula definitesAndConstraints =
                combineWithDefinites(constraintsAsFormulas, pConstraintsToCheck);
            prover.push(definitesAndConstraints);

            stats.timeForProverPreparation.stop();

            return handleSolverResult(
                isUnsat(prover),
                prover,
                relevantConstraints,
                constraintsAsFormulas,
                pConstraintsToCheck);
          }

        } else {
          // Incremental
          stats.persistentProverUsed.inc();
          // def assignments are automatically applied by the persistentProver
          preparePersistentProverForCheck(constraintsAsFormulas);
          stats.timeForProverPreparation.stop();

          boolean unsat = isUnsat(persistentProver);

          // TODO: investigate if we need this with
          // https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1352
          // pop definite assignments. this can only happen after the model produced by the
          // unsat-check
          // is not used anymore
          // prover.pop();

          return handleSolverResult(
              unsat,
              persistentProver,
              relevantConstraints,
              constraintsAsFormulas,
              pConstraintsToCheck);
        }
      }
    } finally {
      stats.timeForSolving.stop();
      stats.timeForProverPreparation.stopIfRunning();
    }
  }

  private boolean isUnsat(ProverEnvironment prover) throws SolverException, InterruptedException {
    try {
      stats.timeForSatCheck.start();
      return prover.isUnsat();
    } finally {
      stats.timeForSatCheck.stop();
    }
  }

  private SolverResult handleSolverResult(
      boolean unsat,
      ProverEnvironment prover,
      ImmutableSet<Constraint> relevantConstraints,
      Collection<BooleanFormula> constraintsAsFormulas,
      ConstraintsState pConstraintsToCheck)
      throws SolverException, InterruptedException {

    if (!unsat) {
      ImmutableList<ValueAssignment> satisfyingModel = prover.getModelAssignments();
      cache.addSat(constraintsAsFormulas, satisfyingModel);

      // doing this while the complete formula is still on the prover environment stack is
      // cheaper than performing another complete SAT check when the assignment is really
      // requested, but also reduces the benefit of reusing the solver as constraints might need
      // to be removed later on
      if (resolveDefinites) {
        ImmutableCollection<ValueAssignment> definiteAssignmentsInModel =
            resolveDefiniteAssignments(pConstraintsToCheck, satisfyingModel, prover);
        assert satisfyingModel.containsAll(definiteAssignmentsInModel)
            : "Model does not imply definites: "
                + satisfyingModel
                + " !=> "
                + definiteAssignmentsInModel;

        return new SolverResult(
            relevantConstraints,
            Satisfiability.SAT,
            Optional.of(satisfyingModel),
            Optional.of(definiteAssignmentsInModel));

      } else {
        return new SolverResult(
            relevantConstraints,
            Satisfiability.SAT,
            Optional.of(satisfyingModel),
            Optional.empty());
      }

    } else {
      cache.addUnsat(constraintsAsFormulas);
      return new SolverResult(
          relevantConstraints, Satisfiability.UNSAT, Optional.empty(), Optional.empty());
    }
  }

  private void preparePersistentProverForCheck(ImmutableSet<BooleanFormula> constraintsToCheck)
      throws InterruptedException {
    AtomicInteger totalKeptRef = new AtomicInteger();
    AtomicInteger totalRemovedRef = new AtomicInteger();

    buildProverStackBasedOnCommonConstraints(constraintsToCheck, totalKeptRef, totalRemovedRef);

    int totalKept = totalKeptRef.get();
    int totalRemoved = totalRemovedRef.get();

    if (totalKept + totalRemoved > 0) {
      stats.reuseRatio.setNextValue((double) totalKept / (totalKept + totalRemoved));
    }

    if (totalRemoved == 0) {
      // We know that we had no cache hit, so it is a constraint combination that we know -> there
      // is new constraints
      stats.persistentProverUsedIncrementallyPushedWithoutPop.inc();
    }

    if (totalKept + totalRemoved == constraintsToCheck.size()) {
      stats.persistentProverUsedIncrementallyFormulasPopdAndNotRepushed.inc();
    }

    for (BooleanFormula f : constraintsToCheck) {
      if (!currentConstraintsOnProver.contains(f)) {
        currentConstraintsOnProver.addLast(f);
        persistentProver.push(f);
      }
    }

    // TODO: replace this with the soon to be public stack from JavaSMT and make it an assertion!
    // assertStack();
    checkState(constraintsToCheck.size() == currentConstraintsOnProver.size());
  }

  // Builds the prover stack incrementally based on the set of common constraints
  private void buildProverStackBasedOnCommonConstraints(
      ImmutableSet<BooleanFormula> constraintsToCheckList,
      AtomicInteger totalKept,
      AtomicInteger totalRemoved)
      throws InterruptedException {

    Set<BooleanFormula> commonConstraints = new HashSet<>(currentConstraintsOnProver);
    commonConstraints.retainAll(constraintsToCheckList);

    // This iterator goes from the bottom of the stack to the top.
    // We check for the first constraint in the stack that's not in the wanted constraints and pop
    // everything >= in the stack and push the wanted constraints.
    Iterator<BooleanFormula> currentStack = currentConstraintsOnProver.iterator();
    int constraintsToCheckListSize = constraintsToCheckList.size();
    int index = 0;
    while (currentStack.hasNext()) {
      BooleanFormula constraintOnStack = currentStack.next();

      if (index >= constraintsToCheckListSize) {
        // pop rest from currentStack, including current index
        int numOfPops = currentConstraintsOnProver.size() - index;
        for (int i = 0; i < numOfPops; i++) {
          currentConstraintsOnProver.removeLast();
          persistentProver.pop();
          totalRemoved.getAndIncrement();
        }
        break;
      }

      // Keep constraints as long as they are in the new stack
      if (commonConstraints.contains(constraintOnStack)) {
        // Keep constraint
        totalKept.getAndIncrement();
        index++;

      } else {
        // Pop all remaining constraints (these might include common ones!)
        int numOfPops = currentConstraintsOnProver.size() - index;
        for (int i = 0; i < numOfPops; i++) {
          currentConstraintsOnProver.removeLast();
          persistentProver.pop();
          totalRemoved.getAndIncrement();
        }
        break;
      }
    }

    // Push all remaining constraints (might include common ones again!)
    Set<BooleanFormula> currentConstraintsOnProverSet = new HashSet<>(currentConstraintsOnProver);
    checkState(currentConstraintsOnProverSet.size() == currentConstraintsOnProver.size());
    for (BooleanFormula constraintToCheck : constraintsToCheckList) {
      if (!currentConstraintsOnProverSet.contains(constraintToCheck)) {
        currentConstraintsOnProver.add(constraintToCheck);
        persistentProver.push(constraintToCheck);
      }
    }

    // TODO: add assertion for stack consistency once the public stack from JavaSMT is available!
    checkState(currentConstraintsOnProver.size() == constraintsToCheckList.size());
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

  // TODO: use distinct prover? Reusing the previous solver is more inefficient on this check, but
  //  might make the subsequent checks more inefficient for persistent provers as constraints need
  //  to be removed. Also, persistent provers know definite assignments already and reuse them
  //  automatically, but it should be assumed that this is only the case as long as the stack is
  //  not popped.
  //  https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1350
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
    return new FormulaCreatorUsingCConverter(machineModel, converter, pFunctionName);
  }

  /**
   * Returns the set of formulas representing all constraints of this state. If no constraints
   * exist, this method will return an empty set.
   *
   * @return the {@link ImmutableSet} of formulas representing all constraints of this state
   * @throws UnrecognizedCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  public ImmutableSet<BooleanFormula> getFullFormula(
      Collection<Constraint> pConstraints, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {

    ImmutableSet.Builder<BooleanFormula> formulasBuilder = ImmutableSet.builder();
    for (Constraint c : pConstraints) {
      if (!constraintFormulas.containsKey(c)) {
        constraintFormulas.put(c, createConstraintFormulas(c, pFunctionName));
      }
      formulasBuilder.add(constraintFormulas.get(c));
    }

    return formulasBuilder.build();
  }

  private BooleanFormula createConstraintFormulas(Constraint pConstraint, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {
    assert !constraintFormulas.containsKey(pConstraint)
        : "Trying to add a formula that already exists!";

    return getFormulaCreator(pFunctionName).createFormula(pConstraint);
  }

  public Solver getSolver() {
    return solver;
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

    SupersetConstraintsCache(final ConstraintsCache pDelegate) {
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

    SubsetConstraintsCache(final ConstraintsCache pDelegate) {
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

    static CacheResult getSat(ImmutableList<ValueAssignment> pModelAssignment) {
      return new CacheResult(Result.SAT, Optional.ofNullable(pModelAssignment));
    }

    static CacheResult getUnsat() {
      return UNSAT_SINGLETON;
    }

    static CacheResult getUnknown() {
      return UNKNOWN_SINGLETON;
    }

    private CacheResult(Result pResult, Optional<ImmutableList<ValueAssignment>> pModelAssignment) {
      result = pResult;
      modelAssignment = pModelAssignment;
    }

    boolean isSat() {
      return result.equals(Result.SAT);
    }

    boolean isUnsat() {
      return result.equals(Result.UNSAT);
    }

    private Optional<ImmutableList<ValueAssignment>> getModelAssignment() {
      checkState(modelAssignment.isPresent());
      return modelAssignment;
    }
  }
}
