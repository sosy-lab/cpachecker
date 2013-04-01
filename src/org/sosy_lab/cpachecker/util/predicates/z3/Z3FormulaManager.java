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

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator.CreateBitType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToInt;

import com.google.common.base.Preconditions;


public class Z3FormulaManager extends AbstractFormulaManager<Long> {

  private long z3context;
  private Z3FormulaCreator creator;

  private Z3FormulaManager(
      AbstractUnsafeFormulaManager<Long> pUnsafeManager,
      AbstractFunctionFormulaManager<Long> pFunctionManager,
      AbstractBooleanFormulaManager<Long> pBooleanManager,
      AbstractRationalFormulaManager<Long> pNumericManager,
      AbstractBitvectorFormulaManager<Long> pBitpreciseManager) {

    super(pUnsafeManager, pFunctionManager, pBooleanManager, pNumericManager, pBitpreciseManager);
    this.creator = (Z3FormulaCreator) getFormulaCreator();
    assert creator != null;
    this.z3context = creator.getEnv();
  }

  public static synchronized Z3FormulaManager create(LogManager logger, Configuration config, boolean useIntegers)
      throws InvalidConfigurationException {

    long cfg = mk_config();
    set_param_value(cfg, "MODEL", "true");
    // TODO add some other params

    // we use the new reference-counting-context,
    // because it will be default soon, 22.03.2013
    final long context = mk_context_rc(cfg);
    del_config(cfg);

    long boolSort = mk_bool_sort(context);
    long numeralSort;
    if (useIntegers) {
      numeralSort = mk_int_sort(context);
    } else {
      numeralSort = mk_real_sort(context);
    }

    CreateBitType<Long> cbt = new CreateBitType<Long>() {

      @Override
      public Long fromSize(int pSize) {
        return mk_bv_sort(context, pSize);
      }
    };

    Z3FormulaCreator creator = new Z3FormulaCreator(context, boolSort, numeralSort, cbt);

    // Create managers
    Z3UnsafeFormulaManager unsafeManager = new Z3UnsafeFormulaManager(creator);
    Z3FunctionFormulaManager functionTheory = new Z3FunctionFormulaManager(creator, unsafeManager);
    Z3BooleanFormulaManager booleanTheory = new Z3BooleanFormulaManager(creator);
    Z3RationalFormulaManager rationalTheory = new Z3RationalFormulaManager(creator, functionTheory);
    Z3BitvectorFormulaManager bitvectorTheory = new Z3BitvectorFormulaManager(creator);

    Z3FormulaManager instance = new Z3FormulaManager(
        unsafeManager, functionTheory, booleanTheory,
        rationalTheory, bitvectorTheory);
    return instance;
  }

  @Override
  public <T extends Formula> T parse(Class<T> pClazz, String pS) throws IllegalArgumentException {
    throw new AssertionError("not implemented");
  }


  static long getZ3Expr(Formula pT) {
    return ((Z3Formula) pT).getExpr();
  }

  @Override
  public String getVersion() {
    PointerToInt major = new PointerToInt();
    PointerToInt minor = new PointerToInt();
    PointerToInt build = new PointerToInt();
    PointerToInt revision = new PointerToInt();
    get_version(major, minor, build, revision);
    return "Z3 " +
        major.value + "." + minor.value + "." +
        build.value + "." + revision.value;
  }

  @Override
  public String dumpFormula(Long expr) {
    return ast_to_string(z3context, expr);
  }

  public long getContext() {
    Preconditions.checkState(z3context != 0);
    return z3context;
  }

  private <T extends Formula> T encapsulateExpr(Class<T> pClazz, long t) {
    return creator.encapsulate(pClazz, t);
  }

  protected <T extends Formula> T encapsulate(Class<T> pClazz, long t) {
    return encapsulateExpr(pClazz, t);
  }

  @Override
  protected Long getTerm(Formula pF) {
    // for visibility
    return super.getTerm(pF);
  }

}