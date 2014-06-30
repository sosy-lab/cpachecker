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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintWriter;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import de.uni_freiburg.informatik.ultimate.logic.Term;


class LoggingSmtInterpolInterpolatingProver extends SmtInterpolInterpolatingProver {

  private final PrintWriter out;

  LoggingSmtInterpolInterpolatingProver(SmtInterpolFormulaManager pMgr,
      PrintWriter pOut) {
    super(pMgr);
    out = checkNotNull(pOut);
  }

  @Override
  protected void pushAndAssert(Term pTerm) {
    out.println("(push 1)");
    out.println(mgr.dumpFormula(pTerm));
    super.pushAndAssert(pTerm);
  }

  @Override
  public void pop() {
    out.println("(pop 1)");
    super.pop();
  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    out.println("(check-sat)");
    return super.isUnsat();
  }

  @Override
  protected BooleanFormula getInterpolant(Term pTermA, Term pTermB) {
    out.println("(get-interpolants " + pTermA + " " + pTermB + ")");
    return super.getInterpolant(pTermA, pTermB);
  }

  @Override
  public void close() {
    out.close();
    super.close();
  }
}
