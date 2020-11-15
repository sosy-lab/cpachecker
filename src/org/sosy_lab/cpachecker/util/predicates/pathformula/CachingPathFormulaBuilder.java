// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import com.google.errorprone.annotations.ForOverride;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Abstract class that can be used to realize {@link PathFormulaBuilder}s that speed up path formula
 * creation by using a cache.
 */
public abstract class CachingPathFormulaBuilder implements PathFormulaBuilder {
  private PathFormula cachedResult = null;
  private PathFormulaManager cachedInputPfmgr = null;
  private PathFormula cachedInputPathFormula = null;

  /**
   * Returns a path formula that was generated with the same input parameters as provided or null.
   *
   * @param pPfmgr the path-formula manager with which the result shall have been computed
   * @param pPathFormula the path formula that shall have been used in the computation of the result
   */
  private final @Nullable PathFormula cachedBuild(
      final PathFormulaManager pPfmgr, final PathFormula pPathFormula) {
    if (cachedResult == null
        || !pPfmgr.equals(cachedInputPfmgr)
        || !pPathFormula.equals(cachedInputPathFormula)) {
      return null;
    }
    return cachedResult;
  }

  private final void updateCache(
      final PathFormula pResult, final PathFormulaManager pPfmgr, final PathFormula pPathFormula) {
    cachedResult = pResult;
    cachedInputPfmgr = pPfmgr;
    cachedInputPathFormula = pPathFormula;
  }

  @ForOverride
  protected abstract PathFormula buildImplementation(
      final PathFormulaManager pPfmgr, final PathFormula pathFormula)
      throws CPATransferException, InterruptedException;

  @Override
  public final PathFormula build(final PathFormulaManager pPfmgr, final PathFormula pathFormula)
      throws CPATransferException, InterruptedException {
    PathFormula result = cachedBuild(pPfmgr, pathFormula);
    if (result == null) {
      result = buildImplementation(pPfmgr, pathFormula);
      updateCache(result, pPfmgr, pathFormula);
    }
    return result;
  }
}
