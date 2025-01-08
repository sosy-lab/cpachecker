// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;

/**
 * Implementation of {@link PathFormulaManager} that delegates to another instance but caches
 * results of some methods.
 */
public class CachingPathFormulaManager implements PathFormulaManager {

  private final Timer pathFormulaComputationTimer = new Timer();
  private int pathFormulaCacheHits = 0;

  public final PathFormulaManager delegate;

  private final Map<
          Pair<Equivalence.Wrapper<CFAEdge>, PathFormula>, Pair<PathFormula, ErrorConditions>>
      andFormulaWithConditionsCache = new HashMap<>();
  private final Map<Pair<Equivalence.Wrapper<CFAEdge>, PathFormula>, PathFormula> andFormulaCache =
      new HashMap<>();

  private final Map<Pair<PathFormula, PathFormula>, PathFormula> orFormulaCache = new HashMap<>();

  private final Map<PathFormula, PathFormula> emptyFormulaCache = new HashMap<>();

  private final PathFormula emptyFormula;

  public CachingPathFormulaManager(PathFormulaManager pDelegate) {
    delegate = pDelegate;
    emptyFormula = delegate.makeEmptyPathFormula();
  }

  /**
   * Returns a cache key for the specified path formula and edge. Uses {@link Equivalence#identity}
   * as an equivalence wrapper for the edge.
   */
  private Pair<Equivalence.Wrapper<CFAEdge>, PathFormula> createFormulaCacheKey(
      PathFormula pOldFormula, CFAEdge pEdge) {
    return Pair.of(Equivalence.identity().wrap(pEdge), pOldFormula);
  }

  @Override
  public Pair<PathFormula, ErrorConditions> makeAndWithErrorConditions(
      PathFormula pOldFormula, CFAEdge pEdge) throws CPATransferException, InterruptedException {
    final Pair<Equivalence.Wrapper<CFAEdge>, PathFormula> formulaCacheKey =
        createFormulaCacheKey(pOldFormula, pEdge);
    Pair<PathFormula, ErrorConditions> result = andFormulaWithConditionsCache.get(formulaCacheKey);
    if (result == null) {
      pathFormulaComputationTimer.start();
      // compute new pathFormula with the operation on the edge
      result = delegate.makeAndWithErrorConditions(pOldFormula, pEdge);
      pathFormulaComputationTimer.stop();
      andFormulaWithConditionsCache.put(formulaCacheKey, result);

    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeAnd(PathFormula pOldFormula, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {
    final Pair<Equivalence.Wrapper<CFAEdge>, PathFormula> formulaCacheKey =
        createFormulaCacheKey(pOldFormula, pEdge);
    PathFormula result = andFormulaCache.get(formulaCacheKey);
    if (result == null) {
      // compute new pathFormula with the operation on the edge
      try {
        pathFormulaComputationTimer.start();
        result = delegate.makeAnd(pOldFormula, pEdge);
        andFormulaCache.put(formulaCacheKey, result);
      } finally {
        pathFormulaComputationTimer.stop();
      }

    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) throws InterruptedException {
    final Pair<PathFormula, PathFormula> formulaCacheKey = Pair.of(pF1, pF2);

    PathFormula result = orFormulaCache.get(formulaCacheKey);
    if (result == null) {
      // try again with other order
      result = orFormulaCache.get(Pair.of(pF2, pF1));
    }

    if (result == null) {
      result = delegate.makeOr(pF1, pF2);
      orFormulaCache.put(formulaCacheKey, result);
    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeConjunction(List<PathFormula> pPathFormulas) {
    return delegate.makeConjunction(pPathFormulas);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return emptyFormula;
  }

  @Override
  public PathFormula makeEmptyPathFormulaWithContextFrom(PathFormula pOldFormula) {
    PathFormula result = emptyFormulaCache.get(pOldFormula);
    if (result == null) {
      result = delegate.makeEmptyPathFormulaWithContextFrom(pOldFormula);
      emptyFormulaCache.put(pOldFormula, result);
    } else {
      pathFormulaCacheHits++;
    }
    return result;
  }

  @Override
  public PathFormula makeEmptyPathFormulaWithContext(SSAMap pSsaMap, PointerTargetSet pPts) {
    return delegate.makeEmptyPathFormulaWithContext(pSsaMap, pPts);
  }

  @Override
  public Formula makeFormulaForVariable(PathFormula pContext, String pVarName, CType pType) {
    return delegate.makeFormulaForVariable(pContext, pVarName, pType);
  }

  @Override
  public Formula makeFormulaForUninstantiatedVariable(
      String pVarName,
      CType pType,
      PointerTargetSet pContextPTS,
      boolean pForcePointerDereference) {
    return delegate.makeFormulaForUninstantiatedVariable(
        pVarName, pType, pContextPTS, pForcePointerDereference);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, BooleanFormula pOtherFormula) {
    return delegate.makeAnd(pPathFormula, pOtherFormula);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, CExpression pAssumption)
      throws CPATransferException, InterruptedException {
    return delegate.makeAnd(pPathFormula, pAssumption);
  }

  @Override
  public PathFormula makeFormulaForPath(List<CFAEdge> pPath)
      throws CPATransferException, InterruptedException {
    return delegate.makeFormulaForPath(pPath);
  }

  @Override
  public ARGPath getARGPathFromModel(
      Model pModel,
      ARGState pRoot,
      Predicate<? super ARGState> pStateFilter,
      Map<Pair<ARGState, CFAEdge>, PathFormula> pBranchingFormulasOverride)
      throws CPATransferException, InterruptedException {
    return delegate.getARGPathFromModel(pModel, pRoot, pStateFilter, pBranchingFormulasOverride);
  }

  @Override
  public void clearCaches() {
    andFormulaWithConditionsCache.clear();
    andFormulaCache.clear();
    orFormulaCache.clear();
    emptyFormulaCache.clear();
    delegate.clearCaches();
  }

  @Override
  public Formula expressionToFormula(PathFormula pFormula, CIdExpression expr, CFAEdge edge)
      throws UnrecognizedCodeException {
    return delegate.expressionToFormula(pFormula, expr, edge);
  }

  @Override
  public BooleanFormula buildImplicationTestAsUnsat(PathFormula pF1, PathFormula pF2)
      throws InterruptedException {
    return delegate.buildImplicationTestAsUnsat(pF1, pF2);
  }

  @Override
  public void printStatistics(PrintStream out) {
    int totalPathFormulaComputations =
        pathFormulaComputationTimer.getNumberOfIntervals() + pathFormulaCacheHits;
    out.println(
        "Number of path formula cache hits:   "
            + pathFormulaCacheHits
            + " ("
            + toPercent(pathFormulaCacheHits, totalPathFormulaComputations)
            + ")");
    out.println();

    out.println("Inside post operator:                  ");
    out.println("  Inside path formula creation:        ");
    out.println("    Time for path formula computation: " + pathFormulaComputationTimer);
    out.println();

    delegate.printStatistics(out);
  }

  @Override
  public BooleanFormula addBitwiseAxiomsIfNeeded(
      final BooleanFormula pMainFormula, final BooleanFormula pExtractionFormula) {
    return delegate.addBitwiseAxiomsIfNeeded(pMainFormula, pExtractionFormula);
  }

  @Override
  public BooleanFormula buildWeakestPrecondition(
      final CFAEdge pEdge, final BooleanFormula pPostcondition)
      throws UnrecognizedCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    return delegate.buildWeakestPrecondition(pEdge, pPostcondition);
  }

  @Override
  public PointerTargetSet mergePts(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pSSA)
      throws InterruptedException {
    return delegate.mergePts(pPts1, pPts2, pSSA);
  }

  @Override
  public PathFormulaBuilder createNewPathFormulaBuilder() {
    return delegate.createNewPathFormulaBuilder();
  }
}
