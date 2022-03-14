// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class tracks constraints which are created during AST traversal but cannot be applied at the
 * time of creation.
 */
public class Constraints {

  private final BooleanFormulaManagerView bfmgr;

  private final List<BooleanFormula> constraints = new ArrayList<>();

  public Constraints(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }

  public void addConstraint(BooleanFormula pCo) {
    constraints.add(pCo);
  }

  public BooleanFormula get() {
    return bfmgr.and(constraints);
  }

  @Override
  public String toString() {
    return constraints.toString();
  }
}
