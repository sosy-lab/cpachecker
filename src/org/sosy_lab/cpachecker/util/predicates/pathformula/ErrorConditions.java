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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;

/**
 * This class tracks conditions under which a memory-related error would occur in the program.
 */
public class ErrorConditions {

  private final BooleanFormulaManagerView bfmgr;

  private BooleanFormula invalidDeref;
  private BooleanFormula invalidFree;

  public ErrorConditions(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
    invalidDeref = bfmgr.makeBoolean(false);
    invalidFree = bfmgr.makeBoolean(false);
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

  static class DummyErrorConditions extends ErrorConditions {

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