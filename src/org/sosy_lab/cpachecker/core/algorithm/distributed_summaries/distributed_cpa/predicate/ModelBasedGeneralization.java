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
 * Rewrites a violation condition as an equivalent disjunction of projected model implicants.
 *
 * <p>A condition {@code φ} denotes {@code ∃ L. φ}, where {@code L} are block-local variables. For
 * each model of {@code φ}, the generalization selects an implicant {@code Λ} and projects its local
 * variables out with {@link ExistentialProjection}. Each resulting cube implies {@code ∃ L. φ}.
 * Enumeration excludes covered models until none remain, so the disjunction also covers every state
 * of the original condition. If the cube limit prevents complete enumeration, the caller retains
 * the original condition.
 *
 * <p>The received precondition supplies an optional vocabulary for rewriting the cubes, but never
 * restricts the states to enumerate. Predecessors may later send different preconditions, so
 * restricting the condition to currently known entry states would lose violations. Vocabulary
 * rewriting likewise covers all states of the cubes and only accepts predicate cubes that imply
 * their disjunction; otherwise it keeps the original cubes.
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
   * Generalizes a violation condition, using the precondition only as a predicate vocabulary.
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
            name -> PredicateOperatorUtil.isLocalVariable(name, ssa, fmgr),
            vocabulary)
        .map(pCondition::withFormula);
  }

  /**
   * Generalizes {@code pCondition} without restricting its entry states, see the class
   * documentation.
   *
   * @param pCondition the formula to generalize
   * @param pIsExistential decides by name which variables of {@code pCondition} are existentially
   *     quantified
   * @param pVocabulary the predicates to rewrite the cubes in, if possible; empty to keep the cubes
   * @return a disjunction of cubes equivalent to {@code ∃ E. pCondition}, or empty if that needs
   *     more than the allowed number of cubes
   */
  Optional<BooleanFormula> generalize(
      BooleanFormula pCondition,
      Predicate<String> pIsExistential,
      ImmutableSet<BooleanFormula> pVocabulary)
      throws InterruptedException, SolverException {
    Set<BooleanFormula> cubes = new LinkedHashSet<>();
    Set<BooleanFormula> excluded = new HashSet<>();
    boolean quantifierFree = true;
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(pCondition);
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
      return Optional.of(rewriteInVocabulary(cubes, pVocabulary));
    }
    return Optional.of(disjunction(cubes));
  }

  /**
   * Rewrites quantifier-free cubes as cubes over the given predicates where possible, see the class
   * documentation.
   *
   * @return a disjunction equivalent to {@code pCubes}; {@code pCubes} as they are if rewriting
   *     needs more than the allowed number of cubes
   */
  private BooleanFormula rewriteInVocabulary(
      Set<BooleanFormula> pCubes, ImmutableSet<BooleanFormula> pVocabulary)
      throws InterruptedException, SolverException {
    BooleanFormula original = disjunction(pCubes);
    Set<BooleanFormula> rewritten = new LinkedHashSet<>();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(original);
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
