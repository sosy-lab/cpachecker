/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.logging;

import java.util.Collection;

import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;


public class LoggingProverEnvironment implements ProverEnvironment {

  private final ProverEnvironment wrapped;
  int level = 0;

  public LoggingProverEnvironment(ProverEnvironment pe) {
    wrapped = pe;
  }

  @Override
  public void push(BooleanFormula f) {
    LoggingFormulaManager.logIndent("TP PUSH in, to LEVEL" + level++);
    wrapped.push(f);
    LoggingFormulaManager.logIndent("TP PUSH out");
  }

  @Override
  public void pop() {
    level--;
    LoggingFormulaManager.logIndent("TP POP in, from LEVEL" + level);
    wrapped.pop();
    LoggingFormulaManager.logIndent("TP POP out");
  }

  @Override
  public boolean isUnsat() {
    LoggingFormulaManager.logIndent("TP CHECK in");
    boolean result = wrapped.isUnsat();
    LoggingFormulaManager.logIndent("TP CHECK out");
    return result;
  }

  @Override
  public Model getModel() throws SolverException {
    LoggingFormulaManager.logIndent("TP MODEL in");
    Model m = wrapped.getModel();
    LoggingFormulaManager.logIndent("TP MODEL: " + m.toString());
    LoggingFormulaManager.logIndent("TP MODEL out");
    return m;
  }

  @Override
  public AllSatResult allSat(Collection<BooleanFormula> important,
      RegionCreator mgr, Timer solveTime, NestedTimer enumTime) {
    LoggingFormulaManager.logIndent("TP ALLSAT in");
    AllSatResult asr = wrapped.allSat(important, mgr, solveTime, enumTime);
    LoggingFormulaManager.logIndent("TP ALLSAT out");
    return asr;
  }

  @Override
  public void close() {
    LoggingFormulaManager.log("TP CLOSE in");
    wrapped.close();
    LoggingFormulaManager.log("TP CLOSE out");
  }

}
