/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;


public interface InvariantSupplier {

  /**
   * Return an invariant that holds at a given node.
   * This method should be relatively cheap and do not block
   * (i.e., do not start an expensive invariant generation procedure).
   *
   * @param node The CFANode.
   * @param fmgr The formula manager which should be used for creating the invariant formula.
   * @param pfmgr The {@link PathFormulaManager} which should be used for creating the invariant formula.
   * @return An invariant boolean formula without SSA indices.
   */
  BooleanFormula getInvariantFor(CFANode node, FormulaManagerView fmgr, PathFormulaManager pfmgr);

  static enum TrivialInvariantSupplier implements InvariantSupplier {
    INSTANCE;

    @Override
    public BooleanFormula getInvariantFor(CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pfmgr) {
      return pFmgr.getBooleanFormulaManager().makeBoolean(true);
    }
  }
}
