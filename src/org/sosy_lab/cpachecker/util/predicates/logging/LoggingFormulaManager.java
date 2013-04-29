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

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;


public class LoggingFormulaManager implements FormulaManager {

  private final FormulaManager wrapped;
  private final LogManager logger;

  public LoggingFormulaManager(LogManager logger, FormulaManager lFmgr) {
    this.wrapped = lFmgr;
    this.logger = logger;
  }

  @Override
  public RationalFormulaManager getRationalFormulaManager() {
    return wrapped.getRationalFormulaManager();
  }

  @Override
  public BooleanFormulaManager getBooleanFormulaManager() {
    return wrapped.getBooleanFormulaManager();
  }

  @Override
  public BitvectorFormulaManager getBitvectorFormulaManager() {
    return wrapped.getBitvectorFormulaManager();
  }

  @Override
  public FunctionFormulaManager getFunctionFormulaManager() {
    return wrapped.getFunctionFormulaManager();
  }

  @Override
  public UnsafeFormulaManager getUnsafeFormulaManager() {
    return wrapped.getUnsafeFormulaManager();
  }

  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T formula) {
    return wrapped.getFormulaType(formula);
  }

  @Override
  public <T extends Formula> T parse(Class<T> clazz, String s) throws IllegalArgumentException {
    return wrapped.parse(clazz, s);
  }

  @Override
  public <T extends Formula> Class<T> getInterface(T instance) {
    return wrapped.getInterface(instance);
  }

  @Override
  public Appender dumpFormula(Formula f) {
    return wrapped.dumpFormula(f);
  }

  @Override
  public String getVersion() {
    return wrapped.getVersion();
  }
}
