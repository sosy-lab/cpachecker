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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;

/**
 * This is a class that allows to use a different SMT solver for interpolation
 * than for the rest.
 * Whenever it is used, it copies the formulas to the interpolation SMT solver
 * and back accordingly.
 */
public class SeparateInterpolatingProverEnvironment<T> implements InterpolatingProverEnvironment<T> {

  private final FormulaManager mainFmgr;
  private final FormulaManager itpFmgr;
  private final InterpolatingProverEnvironment<T> itpEnv;

  public SeparateInterpolatingProverEnvironment(FormulaManager pMainFmgr, FormulaManager pItpFmgr,
      InterpolatingProverEnvironment<T> pItpEnv) {
    mainFmgr = checkNotNull(pMainFmgr);
    itpFmgr = checkNotNull(pItpFmgr);
    itpEnv = checkNotNull(pItpEnv);
  }

  @Override
  public T push(BooleanFormula mainF) {
    BooleanFormula itpF = itpFmgr.parse(mainFmgr.dumpFormula(mainF).toString());
    return itpEnv.push(itpF);
  }

  @Override
  public void pop() {
    itpEnv.pop();
  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    return itpEnv.isUnsat();
  }

  @Override
  public void close() {
    itpEnv.close();
  }

  @Override
  public BooleanFormula getInterpolant(List<T> pFormulasOfA) {
    BooleanFormula itpF = itpEnv.getInterpolant(pFormulasOfA);
    return mainFmgr.parse(itpFmgr.dumpFormula(itpF).toString());
  }

  @Override
  public Model getModel() throws SolverException {
    return itpEnv.getModel();
  }
}
