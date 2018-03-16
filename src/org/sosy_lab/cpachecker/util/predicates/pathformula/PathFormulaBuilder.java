/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This class provides a flexible way of building path formulas.
 * The initial {@link SSAMap} / {@link PathFormula} and the {@link PathFormulaManager}
 * do not have to be known before the corresponding build-method is called.
 * This allows e.g. to easily reorder parts of path formulas and much more.
 */
public class PathFormulaBuilder {

  private static class PathFormulaAndBuilder extends PathFormulaBuilder {

    private PathFormulaBuilder previousPathFormula;

    private CFAEdge edge;

    protected PathFormulaAndBuilder(PathFormulaBuilder pPathFormulaAndBuilder, CFAEdge pEdge) {
      this.previousPathFormula = pPathFormulaAndBuilder;
      this.edge = pEdge;
    }

    @Override
    public PathFormula build(PathFormulaManager pPfmgr, PathFormula pathFormula) throws CPATransferException, InterruptedException {
      return pPfmgr.makeAnd(previousPathFormula.build(pPfmgr, pathFormula), edge);
    }

  }

  private static class PathFormulaOrBuilder extends PathFormulaBuilder {

    private PathFormulaBuilder first;
    private PathFormulaBuilder second;

    protected PathFormulaOrBuilder(PathFormulaBuilder first, PathFormulaBuilder second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public PathFormula build(PathFormulaManager pPfmgr, PathFormula pathFormula) throws CPATransferException, InterruptedException {
      PathFormula result = pPfmgr.makeOr(first.build(pPfmgr, pathFormula),second.build(pPfmgr, pathFormula));
      return result;
    }

  }

   public PathFormulaBuilder makeOr(PathFormulaBuilder other) {
     return new PathFormulaOrBuilder(this, other);
   }

   public PathFormulaBuilder makeAnd(CFAEdge pEdge) {
     return new PathFormulaAndBuilder(this, pEdge);
   }

  /**
   * @throws CPATransferException PathFormulaManager could not create PathFormula
   * @throws InterruptedException PathFormulaManager was interrupted
   */
  @SuppressWarnings("unused")
  public PathFormula build(PathFormulaManager pPfmgr, PathFormula pathFormula) throws CPATransferException,InterruptedException{
    return pathFormula;
 }

}
