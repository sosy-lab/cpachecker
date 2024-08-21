// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** This class tracks conditions under which a memory-related error would occur in the program. */
public class ErrorConditions {

  private final BooleanFormulaManagerView bfmgr;

  private BooleanFormula invalidDeref;
  private BooleanFormula invalidFree;

  public ErrorConditions(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
    invalidDeref = bfmgr.makeFalse();
    invalidFree = bfmgr.makeFalse();
  }

  public boolean isEnabled() {
    return true;
  }

  public void addInvalidDerefCondition(BooleanFormula pCo) {
    invalidDeref = bfmgr.or(invalidDeref, pCo);
  }

  public void addInvalidFreeCondition(BooleanFormula pCo) {
    invalidFree = bfmgr.or(invalidFree, pCo);
  }

  public BooleanFormula getInvalidDerefCondition() {
    return invalidDeref;
  }

  public BooleanFormula getInvalidFreeCondition() {
    return invalidFree;
  }

  public static ErrorConditions dummyInstance(BooleanFormulaManagerView pBfmgr) {
    return new DummyErrorConditions(pBfmgr);
  }

  private static class DummyErrorConditions extends ErrorConditions {

    public DummyErrorConditions(BooleanFormulaManagerView pBfmgr) {
      super(pBfmgr);
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public void addInvalidDerefCondition(BooleanFormula pCo) {
      // disabled
    }

    @Override
    public void addInvalidFreeCondition(BooleanFormula pCo) {
      // disabled
    }
  }
}
