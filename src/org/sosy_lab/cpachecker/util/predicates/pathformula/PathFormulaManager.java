// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;

public interface PathFormulaManager {

  /** Create a new path formula with the formula <code>true</code> and empty SSAMap etc. */
  PathFormula makeEmptyPathFormula();

  /**
   * Create a new path formula with the formula <code>true</code> and SSAMap etc. taken from an
   * existing path formula.
   *
   * <p>This is useful for creating a sequence of path formulas for a path when the new path formula
   * (for the second part of the path) should start with the SSA indices from the old path formula
   * such that both formulas can be conjuncted later on to represent the full path.
   */
  PathFormula makeEmptyPathFormulaWithContextFrom(PathFormula oldFormula);

  /**
   * Create a new path formula with the formula <code>true</code> and the given SSAMap and
   * PointerTargetSet.
   *
   * <p>In most cases {@link #makeEmptyPathFormulaWithContextFrom(PathFormula)} should be used
   * instead, but this method may be useful in similar cases but where no PathFormula instance
   * exists.
   */
  PathFormula makeEmptyPathFormulaWithContext(SSAMap ssaMap, PointerTargetSet pts);

  /**
   * Creates a new path formula representing an OR of the two arguments. Differently from {@link
   * BooleanFormulaManager#or(BooleanFormula, BooleanFormula)}, it also merges the SSA maps and
   * creates the necessary adjustments to the formulas if the two SSA maps contain different values
   * for the same variables.
   *
   * @param pF1 a PathFormula
   * @param pF2 a PathFormula
   * @return (pF1 | pF2)
   */
  PathFormula makeOr(PathFormula pF1, PathFormula pF2) throws InterruptedException;

  PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula);

  PathFormula makeAnd(PathFormula pPathFormula, CExpression pAssumption)
      throws CPATransferException, InterruptedException;

  PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge)
      throws CPATransferException, InterruptedException;

  Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(
      PathFormula oldFormula, CFAEdge edge) throws CPATransferException, InterruptedException;

  PathFormula makeFormulaForPath(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException;

  /**
   * Create a conjunction of path formulas. The result has the conjunction of all formulas, the
   * SSAMap and PointerTargetSet of the last list entry, and the sum of all lengths. If the list is
   * empty, the result is equal to {@link #makeEmptyPathFormula()}).
   *
   * <p>WARNING: The input path formulas must already have matching SSA indices for this to make
   * sense! The usual case to call this method is when you have a list of path formulas, where each
   * path formula except the first was based on the result of a call to {@link
   * #makeEmptyPathFormulaWithContextFrom(PathFormula)} with the previous path formula.
   *
   * <p>Note: This is not a commutative operation! The order of the list matters.
   */
  PathFormula makeConjunction(List<PathFormula> pathFormulas);

  /**
   * Takes a variable name and its type to create the corresponding formula out of it. The <code>
   * pContext</code> is used to supply this method with the necessary {@link SSAMap} and (if
   * necessary) the {@link PointerTargetSet}. The variable is assumed not to be a function
   * parameter, i.e. array won't be treated as pointer variable, but as a constant representing
   * starting address of the array.
   *
   * @param pContext the context in which the variable should be created
   * @param pVarName the name of the variable
   * @param pType the type of the variable
   * @return the created formula, which is always <b>instantiated</b>
   */
  Formula makeFormulaForVariable(PathFormula pContext, String pVarName, CType pType);

  /**
   * Takes a variable name and its type to create the corresponding formula out of it, without
   * adding SSA indices. The <code>pContextPTS</code> is used to supply this method with the
   * necessary {@link PointerTargetSet} for creating appropriate pointer variables. The variable is
   * assumed not to be a function parameter, i.e. array won't be treated as pointer variable, but as
   * a constant representing starting address of the array.
   *
   * @param pVarName the name of the variable
   * @param pType the type of the variable
   * @param pContextPTS the context in which the variable should be created
   * @param forcePointerDereference force the formula to make a pointer dereference (e.g. *UF
   *     main:x)
   * @return the created formula, which is always <b>instantiated</b>
   */
  Formula makeFormulaForUninstantiatedVariable(
      String pVarName, CType pType, PointerTargetSet pContextPTS, boolean forcePointerDereference);

  /**
   * Extract a single path from the ARG that is feasible for the values in a given {@link Model}.
   * The model needs to correspond to something like a BMC query for (a subset of) the ARG. This
   * method is basically like calling {@link ARGUtils#getPathFromBranchingInformation(ARGState,
   * Predicate, java.util.function.BiFunction)} and takes the branching information from the model.
   *
   * @param model The model to use for determining branching information.
   * @param root The root of the ARG, from which the path should start.
   * @return A feasible path through the ARG from root, which conforms to the model.
   */
  default ARGPath getARGPathFromModel(Model model, ARGState root)
      throws CPATransferException, InterruptedException {
    return getARGPathFromModel(model, root, Predicates.alwaysTrue(), ImmutableMap.of());
  }

  /**
   * Extract a single path from the ARG that is feasible for the values in a given {@link Model}.
   * The model needs to correspond to something like a BMC query for (a subset of) the ARG. This
   * method is basically like calling {@link ARGUtils#getPathFromBranchingInformation(ARGState,
   * Predicate, java.util.function.BiFunction)} and takes the branching information from the model.
   *
   * @param model The model to use for determining branching information.
   * @param root The root of the ARG, from which the path should start.
   * @param stateFilter Only consider the subset of ARG states that satisfy this filter.
   * @param branchingFormulasOverride When a formula for the expression of a specific assume edge is
   *     needed, it is first looked up in this map. If not present the formula is created on-the-fly
   *     using the context (SSAMap etc.) from the predicate abstract state inside the {@link
   *     ARGState} at the branching point. The caller needs to ensure that the resulting formulas
   *     match the variables present in the model.
   * @return A feasible path through the ARG from root, which conforms to the model.
   */
  ARGPath getARGPathFromModel(
      Model model,
      ARGState root,
      Predicate<? super ARGState> stateFilter,
      Map<Pair<ARGState, CFAEdge>, PathFormula> branchingFormulasOverride)
      throws CPATransferException, InterruptedException;

  /**
   * Clear all internal caches. Some launches are so huge, that may lead to memory limit, so, in
   * some case it ise useful to reset outdated (and, maybe, necessary) information
   */
  void clearCaches();

  /**
   * Convert a simple C expression to a formula consistent with the current state of the {@code
   * pFormula}.
   *
   * @param pFormula Current {@link PathFormula}.
   * @param expr Expression to convert.
   * @param edge Reference edge, used for log messages only.
   * @return Created formula.
   */
  Formula expressionToFormula(PathFormula pFormula, CIdExpression expr, CFAEdge edge)
      throws UnrecognizedCodeException;

  /**
   * Builds test for PCC that pF1 is covered by more abstract path formula pF2. Assumes that the SSA
   * indices of pF1 are smaller or equal than those of pF2. Since pF1 may be merged with other path
   * formulas resulting in pF2, needs to add assumptions about the connection between indexed
   * variables as included by {@link PathFormulaManager#makeOr(PathFormula, PathFormula)}. Returns
   * negation of implication to check if it is unsatisfiable (implication is valid).
   *
   * @param pF1 path formula which should be covered
   * @param pF2 path formula which covers
   * @return pF1.getFormula() and assumptions and not pF2.getFormula()
   */
  BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2)
      throws InterruptedException;

  /** Prints some information about the PathFormulaManager. */
  void printStatistics(PrintStream out);

  BooleanFormula addBitwiseAxiomsIfNeeded(
      BooleanFormula pMainFormula, BooleanFormula pEsxtractionFormula);

  PathFormulaBuilder createNewPathFormulaBuilder();

  /**
   * Builds a weakest precondition for the given edge and the postcondition
   *
   * @param pEdge Edge containing the statement for the precondition to be built
   * @param pPostcond Postcondition
   * @return Created precondition
   */
  BooleanFormula buildWeakestPrecondition(CFAEdge pEdge, BooleanFormula pPostcond)
      throws UnrecognizedCFAEdgeException, UnrecognizedCodeException, InterruptedException;

  PointerTargetSet mergePts(PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pSSA)
      throws InterruptedException;
}
