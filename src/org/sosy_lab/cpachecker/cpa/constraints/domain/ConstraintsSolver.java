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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.io.PrintStream;
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
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreatorUsingCConverter;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.constraints")
public class ConstraintsSolver implements StatisticsProvider {

  @Option(
    secure = true,
    description = "Whether to use subset/superset caching",
    name = "cacheSubsets"
  )
  private boolean cacheSubsets = true;

  @Option(
      secure = true,
      description = "Whether to perform SAT checks only for the last added constraint",
      name = "minimalSatCheck")
  private boolean performMinimalSatCheck = true;

  @Option(
      secure = true,
      description = "Whether to perform caching of constraint satisfiability results",
      name = "cache"
  )
  private boolean doCaching = true;


  @Option(
      secure = true,
      description = "Resolve definite assignments",
      name = "resolveDefinites"
  )
  private boolean resolveDefinites = true;

  @Option(
      secure = true,
      description = "Try model of predecessor constraints state before running SMT solver",
      name = "useLastModel")
  private boolean useLastModel = true;

  private final LogManagerWithoutDuplicates logger;

  private final StatTimer timeForSatChecks = new StatTimer(StatKind.SUM, "Time for SAT checks");
  private final StatTimer timeForIndependentComputation =
      new StatTimer(StatKind.SUM, "Time for " + "independent computation");
  private final StatTimer timeForDefinitesComputation =
      new StatTimer(StatKind.SUM, "Time for resolving definites");

  private ConstraintsCache cache;
  private Solver solver;
  private ProverEnvironment prover;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;

  private CtoFormulaConverter converter;
  private SymbolicIdentifierLocator locator;

  /** Table of id constraints set, id identifier assignment, formula * */
  private Table<Integer, Integer, BooleanFormula> constraintFormulas = HashBasedTable.create();

  public ConstraintsSolver(
      final Configuration pConfig,
      final LogManager pLogger,
      final Solver pSolver,
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pConverter)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = new LogManagerWithoutDuplicates(pLogger);
    solver = pSolver;
    formulaManager = pFormulaManager;
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    converter = pConverter;
    locator = SymbolicIdentifierLocator.getInstance();

    if (doCaching) {
      if (cacheSubsets) {
        cache = new SubsetConstraintsCache();
      } else {
        cache = new MatchingConstraintsCache();
      }
    } else {
      cache = new DummyCache();
    }

  }

  public boolean isUnsat(
      Constraint pConstraint, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    try {
      timeForSatChecks.start();

      BooleanFormula constraintAsFormula =
          getFullFormula(Collections.singleton(pConstraint), pAssignment, pFunctionName);
      return solver.isUnsat(constraintAsFormula);

    } finally {
      timeForSatChecks.stop();
    }
  }

  /**
   * Returns whether this state is unsatisfiable. A state without constraints (that is, an empty
   * state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public boolean isUnsat(ConstraintsState pConstraints, String pFunctionName)
      throws SolverException, InterruptedException, UnrecognizedCodeException {

    if (!pConstraints.isEmpty()) {
      try {
        timeForSatChecks.start();
        Set<Constraint> relevantConstraints = new HashSet<>();
        if (performMinimalSatCheck && pConstraints.getLastAddedConstraint().isPresent()) {
          try {
            timeForIndependentComputation.start();
            Constraint lastConstraint = pConstraints.getLastAddedConstraint().get();
            Set<Constraint> leftOverConstraints = new HashSet<>(pConstraints);
            Set<SymbolicIdentifier> newRelevantIdentifiers = lastConstraint.accept(locator);
            Set<SymbolicIdentifier> relevantIdentifiers;
            do {
              relevantIdentifiers = ImmutableSet.copyOf(newRelevantIdentifiers);
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
            timeForIndependentComputation.stop();
          }

        } else {
          relevantConstraints = pConstraints;
        }

        BooleanFormula constraintsAsFormula =
            getFullFormula(
                relevantConstraints, pConstraints.getDefiniteAssignment(), pFunctionName);
        prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        prover.push(constraintsAsFormula);

        Boolean unsat = null; // assign null to fail fast if assignment is missed
        ImmutableList<ValueAssignment> modelAsAssignment = pConstraints.getModel();
        ImmutableList<ValueAssignment> newModelAsAssignment = ImmutableList.of();
        if (useLastModel) {
          BooleanFormula lastModel =
              modelAsAssignment
                  .stream()
                  .map(ValueAssignment::getAssignmentAsFormula)
                  .collect(booleanFormulaManager.toConjunction());
          prover.push(lastModel);
          unsat = prover.isUnsat();
          if (!unsat) {
            // get this before popping the model assignment ; the operation will be invalid
            // otherwise
            newModelAsAssignment = prover.getModelAssignments();
          }
          // We have to remove the model assignment before resolving definite assignments, below.
          prover.pop(); // Remove model assignment from prover
        }

        boolean gotResultFromCache = false;

        if (!useLastModel || (unsat && !modelAsAssignment.isEmpty())) {
          // if the last model does not fulfill the formula, and the last model actually
          // is some variable assignment (i.e., model != true), then we check the formula
          // for satisfiability without any assignments, again.
          CacheResult res = cache.getCachedResult(pConstraints);

          if (res.isUnsat()) {
            unsat = true;
            gotResultFromCache = true;
          } else if (res.isSat()) {
            unsat = false;
            gotResultFromCache = true;
            pConstraints.setModel(res.getModelAssignment());
          } else {
            prover.close();
            prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
            prover.push(constraintsAsFormula);
            unsat = prover.isUnsat();

            if (!unsat) {
              newModelAsAssignment = prover.getModelAssignments();
            }
          }
        }

        if (!gotResultFromCache) {
          if (!unsat) {
            pConstraints.setModel(newModelAsAssignment);
            cache.addSat(pConstraints, newModelAsAssignment);
            // doing this while the complete formula is still on the prover environment stack is
            // cheaper than performing another complete SAT check when the assignment is really
            // requested
            if (resolveDefinites) {
              resolveDefiniteAssignments(pConstraints, newModelAsAssignment, pFunctionName);
            }

          } else {
            cache.addUnsat(pConstraints);
          }
        }

        return unsat;

      } finally {
        closeProver();
        timeForSatChecks.stop();
      }

    } else {
      return false;
    }
  }

  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  private void resolveDefiniteAssignments(
      ConstraintsState pConstraints, List<ValueAssignment> pModel, String pFunctionName)
      throws InterruptedException, SolverException {
    try {
      timeForDefinitesComputation.start();

      IdentifierAssignment newDefinites =
          computeDefiniteAssignment(pConstraints, pModel, pFunctionName);
      pConstraints.setDefiniteAssignment(newDefinites);

    } finally {
      timeForDefinitesComputation.stop();
    }
  }

  private IdentifierAssignment computeDefiniteAssignment(
      ConstraintsState pState, List<ValueAssignment> pModel, String pFunctionName)
      throws SolverException, InterruptedException {

    IdentifierAssignment newDefinites = new IdentifierAssignment(pState.getDefiniteAssignment());

    for (ValueAssignment val : pModel) {
      if (SymbolicValues.isSymbolicTerm(val.getName())) {

        SymbolicIdentifier identifier =
            SymbolicValues.convertTermToSymbolicIdentifier(val.getName());
        Value concreteValue = SymbolicValues.convertToValue(val);

        if (!newDefinites.containsKey(identifier)
            && isOnlySatisfyingAssignment(val, pFunctionName)) {

          assert !newDefinites.containsKey(identifier)
                  || newDefinites.get(identifier).equals(concreteValue)
              : "Definite assignment can't be changed from "
                  + newDefinites.get(identifier)
                  + " to "
                  + concreteValue;

          newDefinites.put(identifier, concreteValue);
        }
      } else {
        logger.logOnce(
            Level.FINE, "Constraints solver could not assign value to variable in " + "model");
      }
    }
    assert newDefinites.entrySet().containsAll(pState.getDefiniteAssignment().entrySet());
    return newDefinites;
  }

  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm, String pFunctionName)
      throws SolverException, InterruptedException {

    FormulaCreator formulaCreator = getFormulaCreator(pFunctionName);
    BooleanFormula prohibitAssignment =
        formulaManager.makeNot(
            formulaCreator.transformAssignment(pTerm.getKey(), pTerm.getValue()));

    prover.push(prohibitAssignment);
    boolean isUnsat = prover.isUnsat();

    // remove the just added formula again so we return to the original constraint formula
    // - other assignments will probably be tested before closing prover.
    prover.pop();

    return isUnsat;
  }

  private FormulaCreator getFormulaCreator(String pFunctionName) {
    return new FormulaCreatorUsingCConverter(formulaManager, converter, pFunctionName);
  }

  /**
   * Returns the formula representing the conjunction of all constraints of this state. If no
   * constraints exist, this method will return <code>null</code>.
   *
   * @return the formula representing the conjunction of all constraints of this state
   * @throws UnrecognizedCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  private BooleanFormula getFullFormula(
      Collection<Constraint> pConstraints, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {
    List<BooleanFormula> formulas = new ArrayList<>(pConstraints.size());
    for (Constraint c : pConstraints) {
      int constraintsId = getConstraintId(c);
      int identifierId = getAssignmentId(pAssignment);
      if (!constraintFormulas.contains(constraintsId, identifierId)) {
        constraintFormulas.put(
            constraintsId, identifierId, createConstraintFormulas(c, pAssignment, pFunctionName));
      }
      formulas.add(constraintFormulas.get(constraintsId, identifierId));
    }

    return booleanFormulaManager.and(formulas);
  }

  private int getAssignmentId(IdentifierAssignment pAssignment) {
    return pAssignment.hashCode();
  }

  private int getConstraintId(final Constraint pConstraint) {
    return pConstraint.hashCode();
  }

  private int getConstraintId(Collection<Constraint> pConstraints) {
    int id = 0;
    // do this manually to make sure that we get the same id independent of the data structure
    // used
    for (Constraint c : pConstraints) {
      id += getConstraintId(c);
    }
    return id;
  }

  private BooleanFormula createConstraintFormulas(
      Constraint pConstraint, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {
    assert !constraintFormulas.contains(getConstraintId(pConstraint), getAssignmentId(pAssignment))
        : "Trying to add a formula that already exists!";

    return getFormulaCreator(pFunctionName).createFormula(pConstraint, pAssignment);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {
          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
            StatisticsWriter.writingStatisticsTo(out)
                .put(timeForSatChecks)
                .put(timeForDefinitesComputation);
          }

          @Override
          public String getName() {
            return ConstraintsSolver.class.getSimpleName();
          }
        });

    if (cache instanceof StatisticsProvider) {
      ((StatisticsProvider) cache).collectStatistics(statsCollection);

    } else if (cache instanceof Statistics) {
      statsCollection.add((Statistics) cache);
    }
  }

  private interface ConstraintsCache {
    CacheResult getCachedResult(Collection<Constraint> pConstraints);

    void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment);

    void addUnsat(Collection<Constraint> pConstraints);
  }

  private class MatchingConstraintsCache implements ConstraintsCache, StatisticsProvider {

    private Map<Integer, CacheResult> cacheMap = new HashMap<>();

    private StatCounter cacheLookups = new StatCounter("Matching cache lookups");
    private StatCounter cacheHits = new StatCounter("Matching cache hits");
    private StatTimer lookupTime = new StatTimer("Matching cache lookup time");

    @Override
    public CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      int id = getConstraintId(pConstraints);
      return getCachedResult(id);
    }

    CacheResult getCachedResult(int pId) {
      try {
        cacheLookups.inc();
        lookupTime.start();
        if (cacheMap.containsKey(pId)) {
          cacheHits.inc();
          return cacheMap.get(pId);
        } else {
          return CacheResult.getUnknown();
        }
      } finally {
        lookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints, CacheResult.getSat(pModelAssignment));
    }

    @Override
    public void addUnsat(Collection<Constraint> pConstraints) {
      add(pConstraints, CacheResult.getUnsat());
    }

    private void add(Collection<Constraint> pConstraints, CacheResult pResult) {
      int id = getConstraintId(pConstraints);
      cacheMap.put(id, pResult);
    }

    @Override
    public void collectStatistics(Collection<Statistics> statsCollection) {
      statsCollection.add(
          new Statistics() {
            @Override
            public void printStatistics(
                PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
              StatisticsWriter.writingStatisticsTo(pOut)
                  .put(cacheLookups)
                  .put(cacheHits)
                  .put(lookupTime);
            }

            @Nullable
            @Override
            public String getName() {
              return ConstraintsSolver.this.getClass().getSimpleName();
            }
          });
    }
  }

  private class SubsetConstraintsCache implements ConstraintsCache, StatisticsProvider {

    private MatchingConstraintsCache delegate;

    /**
     * Multimap that maps each constraint to all collections of constraints that it occurred in,
     * where each collection of constraints is represented by a pair &lt;id, size&gt;
     */
    private Multimap<Constraint, Pair<Integer, Integer>> constraintContainedIn =
        HashMultimap.create();

    private StatCounter cacheLookups = new StatCounter("Cache lookups");
    private StatCounter directCacheHits = new StatCounter("Direct cache hits");
    private StatCounter cacheHits = new StatCounter("Subset cache hits");
    private StatTimer lookupTime = new StatTimer(StatKind.SUM, "Subset lookup time");

    public SubsetConstraintsCache() {
      delegate = new MatchingConstraintsCache();
    }

    @Override
    public CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      try {
        cacheLookups.inc();
        lookupTime.start();
        CacheResult res = delegate.getCachedResult(pConstraints);
        if (!res.isSat() && !res.isUnsat()) {
          res = getCachedResultOfSubset(pConstraints);
          if (res.isSat() || res.isUnsat()) {
            cacheHits.inc();
          }
        } else {
          directCacheHits.inc();
        }
        return res;
      } finally {
        lookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints);
      delegate.addSat(pConstraints, pModelAssignment);
    }

    @Override
    public void addUnsat(Collection<Constraint> pConstraints) {
      add(pConstraints);
      delegate.addUnsat(pConstraints);
    }

    private void add(Collection<Constraint> pConstraints) {
      int id = getConstraintId(pConstraints);
      Pair<Integer, Integer> idAndSize = Pair.of(id, pConstraints.size());
      for (Constraint c : pConstraints) {
        constraintContainedIn.put(c, idAndSize);
      }
    }

    CacheResult getCachedResultOfSubset(Collection<Constraint> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Pair<Integer, Integer>> containAllConstraints = null;
      for (Constraint c : pConstraints) {
        Set<Pair<Integer, Integer>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
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
      for (Pair<Integer, Integer> col : containAllConstraints) {
        int idOfCollection = col.getFirst();
        int sizeOfCollection = col.getSecond();
        CacheResult cachedResult = delegate.getCachedResult(idOfCollection);
        if (sizeOfQuery <= sizeOfCollection && cachedResult.isSat()) {
          // currently considered collection is a superset of the queried collection
          return cachedResult;

        } else if (sizeOfQuery >= sizeOfCollection && cachedResult.isUnsat()) {
          // currently considered collection is a subset of the queried collection
          return cachedResult;
        }
      }
      return CacheResult.getUnknown();
    }

    @Override
    public void collectStatistics(Collection<Statistics> statsCollection) {
      statsCollection.add(
          new Statistics() {
            @Override
            public void printStatistics(
                PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
              StatisticsWriter.writingStatisticsTo(pOut)
                  .put(cacheLookups)
                  .put(directCacheHits)
                  .put(cacheHits)
                  .put(lookupTime);
            }

            @Nullable
            @Override
            public String getName() {
              return ConstraintsSolver.this.getClass().getSimpleName();
            }
          });
    }
  }

  private static class DummyCache implements ConstraintsCache {

    @Override
    public CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      return CacheResult.getUnknown();
    }

    @Override
    public void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      // do nothing
    }

    @Override
    public void addUnsat(Collection<Constraint> pConstraints) {
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
      return modelAssignment.get();
    }
  }
}
