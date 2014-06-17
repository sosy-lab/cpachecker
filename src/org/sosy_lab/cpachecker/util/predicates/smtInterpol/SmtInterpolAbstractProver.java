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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;

import static com.google.common.base.Preconditions.checkNotNull;

abstract class SmtInterpolAbstractProver {

  protected Script stack;
  protected SmtInterpolFormulaManager mgr;
  protected final ShutdownNotifier shutdownNotifier;

  protected SmtInterpolAbstractProver(SmtInterpolFormulaManager pMgr, ShutdownNotifier pShutdownNotifier) {
    this.mgr = pMgr;
    this.shutdownNotifier = pShutdownNotifier;
    this.stack = checkNotNull(mgr.getEnvironment().getNewScript());
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  public boolean isUnsat() throws InterruptedException {
    try {
      // We actually terminate SmtInterpol during the analysis
      // by using a shutdown listener. However, SmtInterpol resets the
      // mStopEngine flag in DPLLEngine before starting to solve,
      // so we check here, too.
      shutdownNotifier.shutdownIfNecessary();

      Script.LBool result = stack.checkSat();
      switch (result) {
        case SAT:
          return false;
        case UNSAT:
          return true;
        default:
          shutdownNotifier.shutdownIfNecessary();
          throw new SMTLIBException("checkSat returned " + result);
      }
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public abstract void pop();

  public abstract Model getModel();

  public void close() {
    checkNotNull(stack);
    checkNotNull(mgr);
    stack = null;
    mgr = null;
  }
}