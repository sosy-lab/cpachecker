// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;

/**
 * Rewrites a violation condition as a disjunction of small cubes, each generalized from a model,
 * that together cover the condition wherever the block can actually be entered.
 *
 * <p>A violation condition {@code φ} of a block is read as {@code ∃ L. φ}, where {@code L} are its
 * local variables, and is only ever used by predecessors, which conjoin it with a postcondition of
 * theirs. Those postconditions are what this block received as its precondition {@code P}. The
 * generalization enumerates models of {@code φ ∧ P}. For each model {@code m} it picks the literals
 * of {@code φ} that {@code m} satisfies and that suffice to make {@code φ} true, i.e., an implicant
 * {@code Λ} with {@code m ⊨ Λ} and {@code Λ ⇒ φ}, and projects the local variables out of it with
 * the {@link ExistentialProjection}. {@code Λ} is a plain conjunction, so the one-point rule
 * usually removes every local variable, and the resulting cube only talks about the block entry.
 * The cube is then excluded from the query, and the enumeration stops once the query becomes
 * unsatisfiable.
 *
 * <p>Every cube implies {@code ∃ L. φ}, so every state in the result really reaches the violation
 * and a violation found through it is genuine. At the end, {@code φ ∧ P} implies the disjunction of
 * the cubes, so a predecessor whose postcondition is part of {@code P} gets exactly the same
 * answers from the result as from {@code φ}. The result is weaker than that only outside of {@code
 * P}: it has to be computed anew whenever {@code P} changes, which the block does anyway because a
 * new precondition makes it explore again.
 *
 * <p>Counters and similar integer variables can make every cube a single point. The number of cubes
 * is therefore bounded, and the caller keeps the exact condition if the bound is exceeded.
 *
 * <p>Optionally, the cubes are then rewritten in the vocabulary of the precondition, i.e., over the
 * predicates the predecessors abstract their block ends with. When a predecessor refutes a
 * condition of this block, its refinement adds the interpolants that separate its reachable states
 * from the condition to its precision at the block end, so its next postcondition is expressed in
 * exactly the predicates that decide whether conditions of this block die. For every model of the
 * cubes and the precondition, the rewriting takes the cube of the vocabulary predicates the model
 * satisfies if it implies the cubes, and the cube the model came from otherwise. Either way, every
 * cube still implies {@code ∃ L. φ}, and the result covers the same states of {@code P}. A cube
 * over the predecessor's predicates can cover several cubes at once and does not depend on which
 * path produced it, so conditions become smaller and stay equal across rounds more often.
 */
final class ModelBasedGeneralization {

  /** Prefix for precondition variables that must not be confused with those of the condition. */
  private static final String PRECONDITION_ONLY_PREFIX = "__dss_precondition!";

  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final ExistentialProjection projection;
  private final int maxCubes;
  private final boolean usePreconditionVocabulary;

  ModelBasedGeneralization(
      Solver pSolver,
      ExistentialProjection pProjection,
      int pMaxCubes,
      boolean pUsePreconditionVocabulary) {
    Preconditions.checkArgument(pMaxCubes > 0, "At least one cube must be allowed");
    solver = pSolver;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    projection = pProjection;
    maxCubes = pMaxCubes;
    usePreconditionVocabulary = pUsePreconditionVocabulary;
  }

  /**
   * Generalizes a violation condition relative to the precondition it was found under.
   *
   * @param pCondition the exact violation condition, whose interface consists of the variables at
   *     their latest SSA index
   * @param pPrecondition the uninstantiated precondition of the block exploration that found the
   *     condition
   * @return the generalized condition with the same SSA map, or empty if the condition needs more
   *     cubes than allowed
   */
  Optional<PathFormula> generalize(PathFormula pCondition, BooleanFormula pPrecondition)
      throws InterruptedException, SolverException {
    SSAMap ssa = pCondition.getSsa();
    // The condition describes the block entry by the variables at their latest index, so the
    // precondition has to use exactly these. A variable the condition does not know is
    // unconstrained there, so renaming it apart keeps it from clashing with anything that happens
    // to have the same name, like a nondeterministic value.
    BooleanFormula precondition =
        fmgr.renameFreeVariablesAndUFs(
            fmgr.instantiate(pPrecondition, ssa),
            name ->
                FormulaManagerView.parseName(name).getSecond().isPresent()
                    ? name
                    : PRECONDITION_ONLY_PREFIX + name);
    ImmutableSet<BooleanFormula> vocabulary = ImmutableSet.of();
    if (usePreconditionVocabulary) {
      // predicates over renamed variables say nothing about the condition
      vocabulary =
          FluentIterable.from(fmgr.extractAtoms(precondition, false))
              .filter(
                  atom ->
                      fmgr.extractFunctionNames(atom).stream()
                          .noneMatch(name -> name.startsWith(PRECONDITION_ONLY_PREFIX)))
              .toSet();
    }
    return generalize(
            pCondition.getFormula(),
            precondition,
            name -> PredicateOperatorUtil.isLocalVariable(name, ssa, fmgr),
            vocabulary)
        .map(pCondition::withFormula);
  }

  /**
   * Generalizes {@code pCondition} relative to {@code pPrecondition}, see the class documentation.
   *
   * @param pCondition the formula to generalize
   * @param pPrecondition a formula over the non-existential variables of {@code pCondition}
   * @param pIsExistential decides by name which variables of {@code pCondition} are existentially
   *     quantified
   * @param pVocabulary the predicates to rewrite the cubes in, if possible; empty to keep the cubes
   * @return a disjunction of cubes that implies {@code ∃ E. pCondition} and is implied by it under
   *     {@code pPrecondition}, or empty if that needs more than the allowed number of cubes
   */
  Optional<BooleanFormula> generalize(
      BooleanFormula pCondition,
      BooleanFormula pPrecondition,
      Predicate<String> pIsExistential,
      ImmutableSet<BooleanFormula> pVocabulary)
      throws InterruptedException, SolverException {
    Set<BooleanFormula> cubes = new LinkedHashSet<>();
    Set<BooleanFormula> excluded = new HashSet<>();
    boolean quantifierFree = true;
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bfmgr.and(pCondition, pPrecondition));
      for (int round = 0; !prover.isUnsat(); round++) {
        if (round == maxCubes) {
          return Optional.empty();
        }
        Set<BooleanFormula> implicant;
        try (Model model = prover.getModel()) {
          implicant = new ImplicantBuilder(model).implicantOf(pCondition);
        }
        BooleanFormula cube = projection.project(bfmgr.and(implicant), pIsExistential);
        // If the cube still has existential variables, its negation would be universal, so exclude
        // the implicant instead. That excludes fewer models, but still the current one.
        boolean cubeIsQuantifierFree =
            fmgr.extractFunctionNames(cube).stream().noneMatch(pIsExistential);
        quantifierFree &= cubeIsQuantifierFree;
        BooleanFormula exclusion = cubeIsQuantifierFree ? cube : bfmgr.and(implicant);
        if (!excluded.add(exclusion)) {
          // the model did not satisfy the implicant, e.g., because the solver left an atom
          // undefined; excluding it again would not make progress
          return Optional.empty();
        }
        cubes.add(cube);
        prover.addConstraint(bfmgr.not(exclusion));
      }
    }
    if (quantifierFree && !pVocabulary.isEmpty() && !cubes.isEmpty()) {
      // implication checks against the cubes need them to be free of existential variables
      return Optional.of(rewriteInVocabulary(cubes, pPrecondition, pVocabulary));
    }
    return Optional.of(disjunction(cubes));
  }

  /**
   * Rewrites quantifier-free cubes as cubes over the given predicates where possible, see the class
   * documentation.
   *
   * @return a disjunction of cubes, each implying the disjunction of {@code pCubes}, that covers
   *     all states of {@code pCubes} in {@code pPrecondition}; {@code pCubes} as they are if that
   *     needs more than the allowed number of cubes
   */
  private BooleanFormula rewriteInVocabulary(
      Set<BooleanFormula> pCubes,
      BooleanFormula pPrecondition,
      ImmutableSet<BooleanFormula> pVocabulary)
      throws InterruptedException, SolverException {
    BooleanFormula original = disjunction(pCubes);
    Set<BooleanFormula> rewritten = new LinkedHashSet<>();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(bfmgr.and(original, pPrecondition));
      for (int round = 0; !prover.isUnsat(); round++) {
        if (round == maxCubes) {
          return original;
        }
        BooleanFormula predicateCube;
        BooleanFormula originalCube = null;
        try (Model model = prover.getModel()) {
          ImmutableList.Builder<BooleanFormula> literals = ImmutableList.builder();
          for (BooleanFormula predicate : pVocabulary) {
            literals.add(
                Boolean.FALSE.equals(model.evaluate(predicate)) ? bfmgr.not(predicate) : predicate);
          }
          predicateCube = bfmgr.and(literals.build());
          for (BooleanFormula cube : pCubes) {
            if (Boolean.TRUE.equals(model.evaluate(cube))) {
              originalCube = cube;
              break;
            }
          }
        }
        BooleanFormula taken;
        if (!rewritten.contains(predicateCube) && solver.implies(predicateCube, original)) {
          taken = predicateCube;
        } else if (originalCube != null && !rewritten.contains(originalCube)) {
          taken = originalCube;
        } else {
          // the model left something undefined, so neither choice is known to exclude it
          return original;
        }
        rewritten.add(taken);
        prover.addConstraint(bfmgr.not(taken));
      }
    }
    return disjunction(rewritten);
  }

  private BooleanFormula disjunction(Collection<BooleanFormula> pCubes) {
    return bfmgr.or(
        ImmutableList.sortedCopyOf(Comparator.comparingInt(BooleanFormula::hashCode), pCubes));
  }

  /**
   * Collects literals of a formula that a model satisfies and that imply the formula.
   *
   * <p>The implicant follows the Boolean structure: a conjunction needs all its operands, a
   * disjunction only one that is true in the model, and so on for the other connectives, with
   * negation handled by tracking the polarity. Whatever the model says, the collected literals
   * imply the formula by construction. The model only decides which operand of a disjunction is
   * taken, so that the literals are satisfiable together.
   */
  private final class ImplicantBuilder {

    private final Model model;
    private final Map<BooleanFormula, Boolean> values = new HashMap<>();
    private final Set<BooleanFormula> visitedPositive = new HashSet<>();
    private final Set<BooleanFormula> visitedNegative = new HashSet<>();
    private final Set<BooleanFormula> literals = new LinkedHashSet<>();

    ImplicantBuilder(Model pModel) {
      model = pModel;
    }

    Set<BooleanFormula> implicantOf(BooleanFormula pFormula) {
      collect(pFormula, true);
      return literals;
    }

    /** Adds literals that make {@code pFormula} evaluate to {@code pPolarity}. */
    private void collect(BooleanFormula pFormula, boolean pPolarity) {
      if (!(pPolarity ? visitedPositive : visitedNegative).add(pFormula)) {
        return;
      }
      bfmgr.visit(
          pFormula,
          new BooleanFormulaVisitor<Void>() {
            @Override
            public Void visitConstant(boolean pValue) {
              return null;
            }

            @Override
            public Void visitNot(BooleanFormula pOperand) {
              collect(pOperand, !pPolarity);
              return null;
            }

            @Override
            public Void visitAnd(List<BooleanFormula> pOperands) {
              if (pPolarity) {
                pOperands.forEach(operand -> collect(operand, true));
              } else {
                collect(operandWithValue(pOperands, false), false);
              }
              return null;
            }

            @Override
            public Void visitOr(List<BooleanFormula> pOperands) {
              if (pPolarity) {
                collect(operandWithValue(pOperands, true), true);
              } else {
                pOperands.forEach(operand -> collect(operand, false));
              }
              return null;
            }

            @Override
            public Void visitXor(BooleanFormula pOperand1, BooleanFormula pOperand2) {
              boolean value1 = valueOf(pOperand1);
              collect(pOperand1, value1);
              collect(pOperand2, value1 != pPolarity);
              return null;
            }

            @Override
            public Void visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
              boolean value1 = valueOf(pOperand1);
              collect(pOperand1, value1);
              collect(pOperand2, value1 == pPolarity);
              return null;
            }

            @Override
            public Void visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
              if (pPolarity) {
                if (valueOf(pOperand1)) {
                  collect(pOperand2, true);
                } else {
                  collect(pOperand1, false);
                }
              } else {
                collect(pOperand1, true);
                collect(pOperand2, false);
              }
              return null;
            }

            @Override
            public Void visitIfThenElse(
                BooleanFormula pCondition, BooleanFormula pThen, BooleanFormula pElse) {
              boolean condition = valueOf(pCondition);
              collect(pCondition, condition);
              collect(condition ? pThen : pElse, pPolarity);
              return null;
            }

            @Override
            public Void visitQuantifier(
                Quantifier pQuantifier,
                BooleanFormula pQuantifiedAST,
                List<Formula> pBoundVars,
                BooleanFormula pBody) {
              throw new IllegalArgumentException("Quantified formulas are not supported");
            }

            @Override
            public Void visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
              literals.add(pPolarity ? pAtom : bfmgr.not(pAtom));
              return null;
            }
          });
    }

    /**
     * The first operand with the given value in the model, or the first operand if there is none,
     * which can only happen if the model does not satisfy the formula.
     */
    private BooleanFormula operandWithValue(List<BooleanFormula> pOperands, boolean pValue) {
      for (BooleanFormula operand : pOperands) {
        if (valueOf(operand) == pValue) {
          return operand;
        }
      }
      return pOperands.getFirst();
    }

    /** The value of the formula in the model, computed from the values of its atoms. */
    private boolean valueOf(BooleanFormula pFormula) {
      Boolean cached = values.get(pFormula);
      if (cached != null) {
        return cached;
      }
      boolean value =
          bfmgr.visit(
              pFormula,
              new BooleanFormulaVisitor<Boolean>() {
                @Override
                public Boolean visitConstant(boolean pValue) {
                  return pValue;
                }

                @Override
                public Boolean visitNot(BooleanFormula pOperand) {
                  return !valueOf(pOperand);
                }

                @Override
                public Boolean visitAnd(List<BooleanFormula> pOperands) {
                  for (BooleanFormula operand : pOperands) {
                    if (!valueOf(operand)) {
                      return false;
                    }
                  }
                  return true;
                }

                @Override
                public Boolean visitOr(List<BooleanFormula> pOperands) {
                  for (BooleanFormula operand : pOperands) {
                    if (valueOf(operand)) {
                      return true;
                    }
                  }
                  return false;
                }

                @Override
                public Boolean visitXor(BooleanFormula pOperand1, BooleanFormula pOperand2) {
                  return valueOf(pOperand1) != valueOf(pOperand2);
                }

                @Override
                public Boolean visitEquivalence(
                    BooleanFormula pOperand1, BooleanFormula pOperand2) {
                  return valueOf(pOperand1) == valueOf(pOperand2);
                }

                @Override
                public Boolean visitImplication(
                    BooleanFormula pOperand1, BooleanFormula pOperand2) {
                  return !valueOf(pOperand1) || valueOf(pOperand2);
                }

                @Override
                public Boolean visitIfThenElse(
                    BooleanFormula pCondition, BooleanFormula pThen, BooleanFormula pElse) {
                  return valueOf(pCondition) ? valueOf(pThen) : valueOf(pElse);
                }

                @Override
                public Boolean visitQuantifier(
                    Quantifier pQuantifier,
                    BooleanFormula pQuantifiedAST,
                    List<Formula> pBoundVars,
                    BooleanFormula pBody) {
                  throw new IllegalArgumentException("Quantified formulas are not supported");
                }

                @Override
                public Boolean visitAtom(
                    BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
                  // an atom the model leaves undefined does not matter for satisfying the formula
                  return !Boolean.FALSE.equals(model.evaluate(pAtom));
                }
              });
      values.put(pFormula, value);
      return value;
    }
  }
}
