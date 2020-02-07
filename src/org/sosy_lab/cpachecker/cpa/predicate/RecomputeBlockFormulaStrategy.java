/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class RecomputeBlockFormulaStrategy extends BlockFormulaStrategy {

  private final PathFormulaManager pfmgr;

  public RecomputeBlockFormulaStrategy(final PathFormulaManager pPathFormulaManager) {
    pfmgr = pPathFormulaManager;
  }

  @Override
  BlockFormulas getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates)
      throws CPATransferException, InterruptedException {
    List<BooleanFormula> formulas = new ArrayList<>(abstractionStates.size());

    List<CFAEdge> partialPath;
    ARGState current = argRoot;
    PathFormula previousPathFormula = null;
    for (ARGState next : abstractionStates) {
      final ARGState start = current;
      partialPath = ARGUtils.getOnePathFromTo((x) -> Objects.equals(x, start), next).getFullPath();
      PathFormula partialFormula = pfmgr.makeFormulaForPath(partialPath);
      if (previousPathFormula != null) {
        partialFormula =
            pfmgr.makeNewPathFormula(
                partialFormula,
                previousPathFormula.getSsa(),
                previousPathFormula.getPointerTargetSet());
      }
      formulas.add(partialFormula.getFormula());
      previousPathFormula = partialFormula;
      current = next;
    }
    return new BlockFormulas(formulas);
  }
}
