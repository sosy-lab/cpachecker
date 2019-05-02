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
 */
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
      return cachedResult;
    }
    return null;
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
