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
package org.sosy_lab.cpachecker.util.predicates.pseudoQE;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
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
public class PseudoExistQeManager {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bFmgr;
  private Optional<QuantifiedFormulaManager> qFmgr;

  private final LogManager logger;

  @Option(
    secure = true,
    description = "Use Destructive Equality Resolution as simplification method"
  )
  private boolean useDER = true;

  @Option(secure = true, description = "Use Unconnected Parameter Drop as simplification method")
  private boolean useUPD = true;

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
        "Which solver tactic to use for Quantifier Elimination(Only used if useRealQuantifierElimination=true)"
  )
  private SolverQeTactic solverQeTactic = SolverQeTactic.LIGHT;

  @Option(
    secure = true,
    description =
        "Specify whether to overapproximate quantified formula,"
            + " if one or more quantifiers couldn't be eliminated.(Otherwise an exception will be thrown)"
  )
  private boolean overapprox = true;

  /**
   * Create a new PseudoExistQuantifier elimination manager
   *
   * @param pFmgr The FormulaManager to use for simplification etc.
   * @throws InvalidConfigurationException If the Configuration is invalid
   */
  public PseudoExistQeManager(FormulaManagerView pFmgr, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this, PseudoExistQeManager.class);
    this.fmgr = pFmgr;
    this.bFmgr = fmgr.getBooleanFormulaManager();
    this.logger = pLogger;
    try {
      qFmgr = Optional.of(fmgr.getQuantifiedFormulaManager());
    } catch (UnsupportedOperationException e) {
      qFmgr = Optional.empty();
      this.logger.log(
          Level.WARNING,
          "The selected SMT-Solver does not support Quantifier Elimination, but Solver-based QE is enabled. Switched solverQeTactic configuration option to NONE.");
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
   * @param pQuantifiedFormula The formula in which the
   * @return The quantifier-free formula after performing the specified techniques
   * @throws Exception TODO: Exchange against more fitting Exception
   */
  public BooleanFormula eliminateQuantifiers(
      Map<String, Formula> pQuantifiedVars, BooleanFormula pQuantifiedFormula) throws Exception {
    PseudoExistFormula existFormula =
        new PseudoExistFormula(pQuantifiedVars, pQuantifiedFormula, fmgr);

    // Apply the implemented solver-independent techniques for quantifier elimination one by one.
    // Each time one quantified variable has been removed the previous techniques will be repeated,
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

    // How to handle remaining Quantifiers based on Options and result of previous operations
    if (existFormula.hasQuantifiers()) {
      if (overapprox) {
        logger.log(
            Level.FINE,
            "Successfully eliminated "
                + (pQuantifiedVars.size() - existFormula.getNumberOfQuantifiers())
                + "quantified variable(s), overapproximated formulas containing remaining "
                + existFormula.getNumberOfQuantifiers()
                + "quantified variable(s).");
        return overapproximateFormula(existFormula);
      } else {
        // TODO: Add some better fitting Exception, right now Exception as placeholder.
        // IDEAs: QuantifierEliminationException, FailedQuantifierElimination
        throw new Exception("Failed to eliminate Quantifiers!");
      }
    } else {
      logger.log(Level.FINE, "Sucessfully eliminated all quantified Variables.");
      return existFormula.getInnerFormula();
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
    Set<Formula> boundVars = new HashSet<>(pExistFormula.getQuantifiedVarFormulas());

    FormulaVisitor<Map<Formula, Formula>> visitor =
        new DefaultFormulaVisitor<Map<Formula, Formula>>() {
          @Override
          protected Map<Formula, Formula> visitDefault(Formula pF) {
            return null;
          }

          @Override
          public Map<Formula, Formula> visitFunction(
              Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
            switch (pFunctionDeclaration.getKind()) {
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
        };

    Map<Formula, Formula> potentialReplacement = null;

    // Loop through Conjuncts with quantified Vars
    for (BooleanFormula conjunct : pExistFormula.getConjunctsWithQuantifiedVars()) {
      potentialReplacement = fmgr.visit(conjunct, visitor);
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
      // Filter the old Map of bound variables to create a new PseudoExistFormula
      return new PseudoExistFormula(
          Maps.filterValues(pExistFormula.getQuantifiedVars(), e -> !replacedVar.equals(e)),
          newFormula,
          fmgr);
    } else {
      // Return unchanged, DER is not applicable at this time
      return pExistFormula;
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
   */
  PseudoExistFormula applyUPD(PseudoExistFormula pExistFormula) {
    List<BooleanFormula> conjuncts_with_bound = pExistFormula.getConjunctsWithQuantifiedVars();
    List<BooleanFormula> conjuncts_to_eliminate = new ArrayList<>();
    Map<String, Formula> boundVarsToElim = new HashMap<>(pExistFormula.getQuantifiedVars());

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
      // TODO: Show that the formula F_2 is satisfiable
      BooleanFormula newFormula =
          bFmgr.and(
              bFmgr.and(pExistFormula.getConjunctsWithoutQuantifiedVars()),
              bFmgr.and(
                  FluentIterable.from(conjuncts_with_bound)
                      .filter(Predicates.not(Predicates.in(conjuncts_to_eliminate)))
                      .toList()));

      Map<String, Formula> newBoundVars =
          Maps.filterKeys(
              pExistFormula.getQuantifiedVars(),
              Predicates.not(Predicates.in(boundVarsToElim.keySet())));
      logger.log(
          Level.FINER,
          "Successfully applied UPD to eliminate "
              + (pExistFormula.getNumberOfQuantifiers() - newBoundVars.size())
              + "quantified variable(s).");
      return new PseudoExistFormula(newBoundVars, newFormula, fmgr);
    } else {
      return pExistFormula;
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
    assert qFmgr.isPresent();

    // Create the real quantified formula
    BooleanFormula quantifiedFormula =
        qFmgr
            .get()
            .exists(
                new ArrayList<>(pExistFormula.getQuantifiedVarFormulas()),
                pExistFormula.getInnerFormula());

    BooleanFormula afterQE;
    // Apply the Quantifier elimination tactic
    if (solverQeTactic == SolverQeTactic.LIGHT) {
      afterQE = fmgr.applyTactic(quantifiedFormula, Tactic.QE_LIGHT);
    } else if (solverQeTactic == SolverQeTactic.FULL) {
      try {
        afterQE = qFmgr.get().eliminateQuantifiers(quantifiedFormula);
      } catch (SolverException e) {
        logger.log(
            Level.FINER, "Solver based Quantifier Elimination failed with SolverException!", e);
        // Unable to solve the QE-problem
        afterQE = quantifiedFormula;
      }
    } else {
      afterQE = quantifiedFormula;
    }
    int numberQuantifiers = numberQuantifiers(afterQE);
    // Check if number of quantified vars less than before
    if (numberQuantifiers < pExistFormula.getNumberOfQuantifiers()) {
      PseudoExistFormula result;

      if (numberQuantifiers == 0) {
        // If no more quantifiers just return the result of QE
        result = new PseudoExistFormula(new HashMap<>(), afterQE, fmgr);
        logger.log(
            Level.FINER,
            "Successfully applied Solver-QE to eliminate "
                + pExistFormula.getNumberOfQuantifiers()
                + "quantified variable(s).");
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
}
