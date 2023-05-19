// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pseudoQE;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * Manager to apply several solver-independent techniques to eliminate existential quantifiers.
 *
 * <p><b>NOTE:</b> Right now only conjunctions of quantifier free Predicates are expected as input,
 * for all other cases the behavior is not tested and probably not sufficient.
 */
@Options(prefix = "cpa.predicate.pseudoExistQE")
public class PseudoExistQeManager implements StatisticsProvider {
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bFmgr;
  private Optional<QuantifiedFormulaManager> qFmgr;

  private final LogManager logger;
  private final PseudoExQeStatistics stats = new PseudoExQeStatistics();

  @Option(
      secure = true,
      description = "Use Destructive Equality Resolution as simplification method")
  private boolean useDER = true;

  @Option(secure = true, description = "Use Unconnected Parameter Drop as simplification method")
  private boolean useUPD = true;

  /**
   * This class visits one level of a formula and returns the pair of bound variable and its equal
   * part if found, and NULL otherwise.
   */
  private static final class EqualityExtractor
      extends DefaultFormulaVisitor<Map<Formula, Formula>> {

    private final Set<Formula> boundVars;

    private EqualityExtractor(Set<Formula> pBoundVars) {
      boundVars = pBoundVars;
    }

    @Override
    protected Map<Formula, Formula> visitDefault(Formula pF) {
      return null;
    }

    @Override
    public Map<Formula, Formula> visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
      switch (pFunctionDeclaration.getKind()) {

          // TODO this code assumes that equality has exactly two arguments.
          // We might have more than two.

        case EQ: // check those functions that represent equality
        case BV_EQ:
        case FP_EQ:
          if (boundVars.contains(pArgs.get(0))) {
            return ImmutableMap.of(pArgs.get(0), pArgs.get(1));
          } else if (boundVars.contains(pArgs.get(1))) {
            return ImmutableMap.of(pArgs.get(1), pArgs.get(0));
          } else {
            return null;
          }
        default:
          return null;
      }
    }
  }

  enum SolverQeTactic {
    /** Don't use Solver Quantifier Elimination */
    NONE,
    /** Use Light Quantifier Elimination as implemented in used solver */
    LIGHT,
    /** Use Full Quantifier Elimination as implemented in used solver */
    FULL
  }

  @Option(
      secure = true,
      description =
          "Which solver tactic to use for Quantifier Elimination(Only used if"
              + " useRealQuantifierElimination=true)")
  private SolverQeTactic solverQeTactic = SolverQeTactic.LIGHT;

  @Option(
      secure = true,
      description =
          "Specify whether to overapproximate quantified formula, if one or more quantifiers"
              + " couldn't be eliminated.(Otherwise an exception will be thrown)")
  private boolean overapprox = false;

  /**
   * Create a new PseudoExistQuantifier elimination manager
   *
   * @param pSolver The Solver to use
   * @param pConfig The configuration to use
   * @param pLogger The logger instance to use
   * @throws InvalidConfigurationException If the Configuration is invalid
   */
  public PseudoExistQeManager(Solver pSolver, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this, PseudoExistQeManager.class);
    solver = pSolver;
    fmgr = pSolver.getFormulaManager();
    bFmgr = fmgr.getBooleanFormulaManager();
    logger = pLogger;
    try {
      qFmgr = Optional.of(fmgr.getQuantifiedFormulaManager());
    } catch (UnsupportedOperationException e) {
      qFmgr = Optional.empty();
      logger.log(
          Level.WARNING,
          "The selected SMT-Solver does not support Quantifier Elimination, but Solver-based QE is"
              + " enabled. Switched solverQeTactic configuration option to NONE.");
      solverQeTactic = SolverQeTactic.NONE;
    }
  }

  /**
   * Eliminate the Quantifiers in a formula based on the techniques selected in the configuration.
   * Designed to avoid Solver independent Quantifier-elimination and combining these techniques with
   * build-in tactics
   *
   * <p><b>NOTE:</b> Right now only conjunctions of quantifier free Predicates are expected as
   * input, for all other cases the behavior is not tested and probably not sufficient.
   *
   * @param pQuantifiedVars A map containing the names of the variables that would be quantified as
   *     keys and the corresponding Formulas as values
   * @param pQuantifiedFormula The formula in which the variables are bound
   * @return Either an optional containing quantifier-free formula after performing the specified
   *     techniques or an empty optional if the quantifier elimination was not possible
   * @throws InterruptedException If interrupted
   */
  public Optional<BooleanFormula> eliminateQuantifiers(
      Map<String, Formula> pQuantifiedVars, BooleanFormula pQuantifiedFormula)
      throws InterruptedException {
    stats.qeTimer.start();
    try {
      stats.qeTotalCounter += pQuantifiedVars.size();
      PseudoExistFormula existFormula =
          new PseudoExistFormula(pQuantifiedVars, pQuantifiedFormula, fmgr);

      // Apply the implemented solver-independent techniques for quantifier elimination one by one.
      // Each time one quantified variable has been removed the previous techniques will be
      // repeated,
      // on the new smaller Set of quantified Variables
      int quantifierCountLastIteration = Integer.MAX_VALUE;

      while ((quantifierCountLastIteration > existFormula.getNumberOfQuantifiers())
          && existFormula.hasQuantifiers()) {
        quantifierCountLastIteration = existFormula.getNumberOfQuantifiers();
        if (useDER && existFormula.hasQuantifiers()) {
          existFormula = applyDER(existFormula);
        }
        if (useUPD && existFormula.hasQuantifiers()) {
          existFormula = applyUPD(existFormula);
        }
      }

      // Use Solver-Build-In Quantifier elimination techniques if supported
      if (solverQeTactic != SolverQeTactic.NONE && existFormula.hasQuantifiers()) {
        existFormula = applyRealQuantifierElimination(existFormula);
      }

      int numberOfEliminatedVariables =
          pQuantifiedVars.size() - existFormula.getNumberOfQuantifiers();
      stats.qeSuccessCounter += numberOfEliminatedVariables;
      // How to handle remaining Quantifiers based on Options and result of previous operations
      if (numberOfEliminatedVariables < 1) {
        if (overapprox) {
          logger.logf(
              Level.FINE,
              "Successfully eliminated %d quantified variable(s), "
                  + "overapproximated formulas containing remaining %d quantified variable(s).",
              numberOfEliminatedVariables,
              existFormula.getNumberOfQuantifiers());
          return Optional.of(overapproximateFormula(existFormula));
        } else {
          return Optional.empty();
        }
      } else {
        logger.log(Level.FINE, "Sucessfully eliminated all quantified Variables.");
        return Optional.of(existFormula.getInnerFormula());
      }
    } finally {
      stats.qeTimer.stop();
    }
  }

  /**
   * Apply the "Destructive Equality Resolution" on the Formula
   *
   * @param pExistFormula The Formula to eliminate quantifiers in
   * @return If possible a DER-simplified Formula, else it returns the input formula
   * @throws InterruptedException when interrupted
   */
  PseudoExistFormula applyDER(PseudoExistFormula pExistFormula) throws InterruptedException {
    stats.derTimer.start();
    try {
      Set<Formula> boundVars = ImmutableSet.copyOf(pExistFormula.getQuantifiedVarFormulas());

      FormulaVisitor<Map<Formula, Formula>> visitor = new EqualityExtractor(boundVars);

      Map<Formula, Formula> potentialReplacement = null;

      // Loop through Conjuncts with quantified Vars
      for (BooleanFormula conjunct : pExistFormula.getConjunctsWithQuantifiedVars()) {
        potentialReplacement = fmgr.visit(conjunct, visitor);
        // abort after the first available replacement
        if (potentialReplacement != null) {
          break;
        }
      }

      if (potentialReplacement != null) {
        // As implementation only allows 1 replacement at a time
        assert potentialReplacement.size() == 1;

        // Substitute the bound variable by the term in entire inner formula
        BooleanFormula newFormula =
            fmgr.substitute(pExistFormula.getInnerFormula(), potentialReplacement);
        Formula replacedVar = Iterables.getOnlyElement(potentialReplacement.keySet());
        newFormula = fmgr.simplify(newFormula);

        logger.log(Level.FINER, "Successfully applied DER to eliminate 1 quantified variable.");
        stats.derSucessCounter += 1;

        // Filter the old Map of bound variables to create a new PseudoExistFormula
        return new PseudoExistFormula(
            Maps.filterValues(pExistFormula.getQuantifiedVars(), e -> !replacedVar.equals(e)),
            newFormula,
            fmgr);
      } else {
        // Return unchanged, DER is not applicable at this time
        return pExistFormula;
      }
    } finally {
      stats.derTimer.stop();
    }
  }

  /**
   * Apply the "Unconnected Parameter Drop" on the Formula.
   *
   * <ul>
   *   <li>ex v_1,...v_n.F_1 and F_2 with:
   *   <li>- F_2 contains only bound variables not occuring in F_1 and Theory axioms
   *   <li>=> (ex v_1,...v_n.F_1 and F_2) == F_1
   * </ul>
   *
   * @param pExistFormula The Formula to eliminate quantifiers in
   * @return If possible a UPD-simplified Formula, else it returns the input formula
   * @throws InterruptedException When interrupted
   */
  PseudoExistFormula applyUPD(PseudoExistFormula pExistFormula) throws InterruptedException {
    stats.updTimer.start();
    try {
      List<BooleanFormula> conjuncts_with_bound = pExistFormula.getConjunctsWithQuantifiedVars();
      List<BooleanFormula> conjuncts_to_eliminate = new ArrayList<>();
      Map<String, Formula> boundVarsToElim = new LinkedHashMap<>(pExistFormula.getQuantifiedVars());

      for (BooleanFormula conjunct : conjuncts_with_bound) {
        Set<String> varNames = fmgr.extractVariableNames(conjunct);
        Set<String> boundVarNames = Sets.intersection(varNames, boundVarsToElim.keySet());
        if (varNames.equals(boundVarNames)) {
          // The bound vars maybe can be eliminated
          conjuncts_to_eliminate.add(conjunct);
        } else {
          // The bound vars in this conjunct cannot be eliminated with UPD
          boundVarsToElim =
              Maps.filterKeys(boundVarsToElim, Predicates.not(Predicates.in(boundVarNames)));
        }
      }

      if (!boundVarsToElim.isEmpty()) {
        // Show that the conjuncts to remove are satisfiable
        try {
          if (solver.isUnsat(bFmgr.and(conjuncts_to_eliminate))) {
            return pExistFormula;
          }
        } catch (SolverException e) {
          logger.log(
              Level.WARNING,
              "Solver failed while proving satisfiability of unconnected conjuncts. Ignore"
                  + " UPD-result.");
          return pExistFormula;
        }
        // Create resulting inner Formula
        BooleanFormula newFormula =
            bFmgr.and(
                bFmgr.and(pExistFormula.getConjunctsWithoutQuantifiedVars()),
                bFmgr.and(
                    FluentIterable.from(conjuncts_with_bound)
                        .filter(Predicates.not(Predicates.in(conjuncts_to_eliminate)))
                        .toList()));

        // newBoundVars = oldBoundVars \ boundVarstoElim
        Map<String, Formula> newBoundVars =
            Maps.filterKeys(
                pExistFormula.getQuantifiedVars(),
                Predicates.not(Predicates.in(boundVarsToElim.keySet())));

        logger.log(
            Level.FINER,
            "Successfully applied UPD to eliminate "
                + (pExistFormula.getNumberOfQuantifiers() - newBoundVars.size())
                + "quantified variable(s).");
        stats.updSucessCounter += pExistFormula.getNumberOfQuantifiers() - newBoundVars.size();
        return new PseudoExistFormula(newBoundVars, newFormula, fmgr);
      } else {
        return pExistFormula;
      }
    } finally {
      stats.updTimer.stop();
    }
  }

  /**
   * Apply solver-integrated quantifier elimination on the Formula
   *
   * @param pExistFormula The Formula to eliminate quantifiers in
   * @return The Formula after applying Solver-QE or in case of an solver Exception the input
   *     Formula
   * @throws InterruptedException When interrupted
   */
  PseudoExistFormula applyRealQuantifierElimination(PseudoExistFormula pExistFormula)
      throws InterruptedException {
    stats.solverQETimer.start();
    try {
      assert qFmgr.isPresent();

      // Create the real quantified formula
      BooleanFormula quantifiedFormula =
          qFmgr
              .orElseThrow()
              .exists(
                  new ArrayList<>(pExistFormula.getQuantifiedVarFormulas()),
                  pExistFormula.getInnerFormula());

      BooleanFormula afterQE = quantifiedFormula;
      // Apply the Quantifier elimination tactic
      if (solverQeTactic == SolverQeTactic.LIGHT || solverQeTactic == SolverQeTactic.FULL) {
        afterQE = fmgr.applyTactic(quantifiedFormula, Tactic.QE_LIGHT);
      }
      if (solverQeTactic == SolverQeTactic.FULL) {
        try {
          afterQE = qFmgr.orElseThrow().eliminateQuantifiers(quantifiedFormula);
        } catch (SolverException e) {
          logger.log(
              Level.FINER, "Solver based Quantifier Elimination failed with SolverException!", e);
        }
      }
      int numberQuantifiers = numberQuantifiers(afterQE);
      // Check if number of quantified vars less than before
      if (numberQuantifiers < pExistFormula.getNumberOfQuantifiers()) {
        PseudoExistFormula result;

        if (numberQuantifiers == 0) {
          // If no more quantifiers just return the result of QE
          result = new PseudoExistFormula(ImmutableMap.of(), afterQE, fmgr);
          logger.log(
              Level.FINER,
              "Successfully applied Solver-QE to eliminate "
                  + pExistFormula.getNumberOfQuantifiers()
                  + "quantified variable(s).");
          stats.solverQeSucessCounter += pExistFormula.getNumberOfQuantifiers();
        } else {
          // Extract Formula and map of quantified vars and create new Formula

          // TODO:    1. extract the Variable names of the still bound variables
          //          2. replace the bound vars in the inner formula by the unbound variables
          //          3. build pseudoQuantified Formula
          result = pExistFormula;
        }
        // Ensure the inner formula does not contain anymore Quantifiers,
        // else fallback to the inputFormula
        if (isQuantified(result.getInnerFormula())) {
          result = pExistFormula;
        }
        return result;
      } else {
        // Return unchanged input
        return pExistFormula;
      }
    } finally {
      stats.solverQETimer.stop();
    }
  }

  /**
   * Over-approximate the Existential Formula by dropping conjuncts containing quantified variables
   *
   * @param pExistFormula The Pseudo Existential Formula to overapproximate
   * @return The over-approximated quantifier-free BooleanFormula
   */
  BooleanFormula overapproximateFormula(PseudoExistFormula pExistFormula) {
    return bFmgr.and(pExistFormula.getConjunctsWithoutQuantifiedVars());
  }

  /**
   * Checks the number of top-level quantifiers.
   *
   * <p><b>Note:</b> Does not work recursive, so only counts bound variables in the outermost
   * BooleanFormula
   *
   * <p>Question: Better in qFmgr?
   *
   * @param pAfterQE the formula
   * @return True if quantified, False if quantifier-free
   */
  int numberQuantifiers(BooleanFormula pAfterQE) {
    Integer numberQuantifiers =
        fmgr.visit(
            pAfterQE,
            new DefaultFormulaVisitor<Integer>() {

              @Override
              protected Integer visitDefault(Formula pF) {
                return 0;
              }

              @Override
              public Integer visitQuantifier(
                  BooleanFormula pF,
                  Quantifier pQ,
                  List<Formula> pBoundVariables,
                  BooleanFormula pBody) {
                assert !pBoundVariables.isEmpty();
                return pBoundVariables.size();
              }
            });
    return numberQuantifiers;
  }

  /**
   * Check whether a Formula contains Quantifiers(Recursive) Question: Better in qFmgr?
   *
   * <p>Question: Better in qFmgr?
   *
   * @param pAfterQE the formula
   * @return True if quantified, False if quantifier-free
   */
  boolean isQuantified(BooleanFormula pAfterQE) {
    AtomicBoolean foundQuantifier = new AtomicBoolean();
    // Check if Formula contains any quantified vars
    fmgr.visitRecursively(
        pAfterQE,
        new DefaultFormulaVisitor<TraversalProcess>() {

          @Override
          protected TraversalProcess visitDefault(Formula pF) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitQuantifier(
              BooleanFormula pF,
              Quantifier pQ,
              List<Formula> pBoundVariables,
              BooleanFormula pBody) {
            foundQuantifier.set(true);
            return TraversalProcess.ABORT;
          }
        });
    return foundQuantifier.get();
  }

  private class PseudoExQeStatistics implements Statistics {
    // Counter
    private int qeTotalCounter = 0;
    private int qeSuccessCounter = 0;
    private int derSucessCounter = 0;
    private int updSucessCounter = 0;
    private int solverQeSucessCounter = 0;

    // Timer
    private final Timer qeTimer = new Timer();
    private final Timer derTimer = new Timer();
    private final Timer updTimer = new Timer();
    private final Timer solverQETimer = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Number of QEs                 : " + qeTotalCounter);
      if (qeTotalCounter > 0) {
        pOut.println("Successful QEs              : " + qeSuccessCounter);
        pOut.println("Time spent QE               : " + qeTimer);
        if (useDER) {
          pOut.println("  Sucessful DER             : " + derSucessCounter);
          pOut.println("  Time spent DER            : " + derTimer);
        }
        if (useUPD) {
          pOut.println("  Sucessful UPD             : " + updSucessCounter);
          pOut.println("  Time spent UPD            : " + updTimer);
        }
        if (solverQeTactic != SolverQeTactic.NONE) {
          pOut.println("  Sucessful Solver Based QEs: " + solverQeSucessCounter);
          pOut.println("  Time spent Solver Based QE: " + solverQETimer);
        }
      }
    }

    @Override
    public @Nullable String getName() {
      return "Quantifier Elimination";
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
