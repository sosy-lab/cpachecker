/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;

/**
 * This class tracks constraints which are created during AST traversal but
 * cannot be applied at the time of creation.
 */
class Constraints {

  private final BooleanFormulaManagerView bfmgr;

  private final List<BooleanFormula> constraints = new ArrayList<>();

  /**
   * @param pCtoFormulaConverter
   */
  Constraints(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  void addConstraint(BooleanFormula pCo) {
    constraints.add(pCo);
  }

  public BooleanFormula get() {
    return bfmgr.and(constraints);
  }
}