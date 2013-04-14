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

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;


public class LoggingInterpolatingProverEnvironment<T> implements InterpolatingProverEnvironment<T> {

  private final InterpolatingProverEnvironment<T> wrapped;

  public LoggingInterpolatingProverEnvironment(InterpolatingProverEnvironment<T> ipe) {
    wrapped = ipe;
  }

  @Override
  public T push(BooleanFormula f) {
    LoggingFormulaManager.logIndent("ITPP PUSH in");
    T result = wrapped.push(f);
    LoggingFormulaManager.logIndent("ITPP PUSH out");
    return result;
  }

  @Override
  public void pop() {
    LoggingFormulaManager.logIndent("ITPP POP in");
    wrapped.pop();
    LoggingFormulaManager.logIndent("ITPP POP out");
  }

  @Override
  public boolean isUnsat() {
    LoggingFormulaManager.logIndent("ITPP CHECK in");
    boolean result = wrapped.isUnsat();
    LoggingFormulaManager.logIndent("ITPP CHECK out");
    return result;
  }

  @Override
  public BooleanFormula getInterpolant(List<T> formulasOfA) {
    LoggingFormulaManager.logIndent("ITPP ITP in");
    BooleanFormula bf = wrapped.getInterpolant(formulasOfA);
    LoggingFormulaManager.logIndent("ITPP ITP out");
    return bf;
  }

  @Override
  public Model getModel() throws SolverException {
    LoggingFormulaManager.logIndent("ITPP MODEL in");
    Model m = wrapped.getModel();
    LoggingFormulaManager.logIndent("ITPP MODEL out");
    return m;
  }

  @Override
  public void close() {
    LoggingFormulaManager.log("ITPP CLOSE in");
    wrapped.close();
    LoggingFormulaManager.log("ITPP CLOSE out");
  }

}
