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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;


public class Z3FormulaManager extends AbstractFormulaManager<Long> {

  private final long ctx;
  private final Z3Settings settings;

  public Z3FormulaManager(Z3UnsafeFormulaManager pUnsafeManager,
      Z3FunctionFormulaManager pFunctionManager,
      Z3BooleanFormulaManager pBooleanManager,
      Z3RationalFormulaManager pRationalManager,
      Z3BitvectorFormulaManager pBitvectorManager,
      Z3Settings pSettings) {
    super(pUnsafeManager, pFunctionManager, pBooleanManager,
        pRationalManager, pBitvectorManager);
    this.ctx = getFormulaCreator().getEnv();
    this.settings = pSettings;
  }

  @Override
  protected Z3FormulaCreator getFormulaCreator() {
    return (Z3FormulaCreator) super.getFormulaCreator();
  }

  @Override
  public <T extends Formula> T parse(Class<T> pClazz, String pS) throws IllegalArgumentException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getVersion() {
    Native.IntPtr major = new Native.IntPtr();
    Native.IntPtr minor = new Native.IntPtr();
    Native.IntPtr build = new Native.IntPtr();
    Native.IntPtr revision = new Native.IntPtr();
    Native.getVersion(major, minor, build, revision);
    return String.format("%d.%d.%d.%d", major.value, minor.value, build.value, revision.value);
  }

  @Override
  public String dumpFormula(Long pT) {
    try {
      return Native.astToString(ctx, pT);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  long createContext(long cfg) {
    try {
      for (Map.Entry<String, String> option : settings.furtherOptionsMap.entrySet())
        Native.updateParamValue(cfg, option.getKey(), option.getValue());
      return Native.mkContext(cfg);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

}
