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
package org.sosy_lab.cpachecker.util.predicates.princess;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.predicates.Model;

abstract class PrincessAbstractProver {

  protected PrincessStack stack;
  protected PrincessFormulaManager mgr;

  protected PrincessAbstractProver(PrincessFormulaManager pMgr, boolean useForInterpolation) {
    this.mgr = pMgr;
    this.stack = checkNotNull(mgr.getEnvironment().getNewStack(useForInterpolation));
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  public boolean isUnsat() {
    return !stack.checkSat();
  }

  public abstract void pop();

  public abstract Model getModel();

  public void close() {
    checkNotNull(stack);
    checkNotNull(mgr);
    stack.close();
    stack = null;
    mgr = null;
  }
}