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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToInt;

import com.google.common.base.Preconditions;

@Options(prefix = "cpa.predicate.solver.z3")
public class Z3FormulaManager extends AbstractFormulaManager<Long> {

  @Option(description = "simplify formulas when they are asserted in a solver.")
  boolean simplifyFormulas = false;

  private final long z3context;
  private final Z3FormulaCreator creator;
  private final Z3SmtLogger z3smtLogger;

  private Z3FormulaManager(
      AbstractUnsafeFormulaManager<Long> pUnsafeManager,
      AbstractFunctionFormulaManager<Long> pFunctionManager,
      AbstractBooleanFormulaManager<Long> pBooleanManager,
      AbstractRationalFormulaManager<Long> pNumericManager,
      AbstractBitvectorFormulaManager<Long> pBitpreciseManager,
      Z3SmtLogger smtLogger, Configuration config) throws InvalidConfigurationException {

    super(pUnsafeManager, pFunctionManager, pBooleanManager, pNumericManager, pBitpreciseManager);

    config.inject(this);

    this.creator = (Z3FormulaCreator) getFormulaCreator();
    assert creator != null;
    this.z3context = creator.getEnv();
    this.z3smtLogger = smtLogger;
  }

  public static synchronized Z3FormulaManager create(LogManager logger, Configuration config, boolean useIntegers)
      throws InvalidConfigurationException {

    /*
    Following method is part of the file "api_interp.cpp" from Z3.
    It returns a default context, only some params are set.
    We set the same params in a default context,
    so that interpolation is possible.

    Z3_context Z3_mk_interpolation_context(Z3_config cfg){
      if(!cfg) cfg = Z3_mk_config();
      Z3_set_param_value(cfg, "PROOF", "true");
      Z3_set_param_value(cfg, "MODEL", "true");
      Z3_context ctx = Z3_mk_context(cfg);
      Z3_del_config(cfg);
      return ctx;
    }
    */

    //    open_log("z3output.log"); // dumps some log in a special z3-format

    long cfg = mk_config();
    set_param_value(cfg, "MODEL", "true"); // this option is needed also without interpolation
    set_param_value(cfg, "PROOF", "true");

    //    set_param_value(cfg, "trace", "true");
    //    set_param_value(cfg, "trace_file_name", "z3_internal.log");

    // TODO add some other params, memory-limit?

    // we use the new reference-counting-context,
    // because it will be default sometimes in future, 22.03.2013
    final long context = mk_context_rc(cfg);
    del_config(cfg);

    long boolSort = mk_bool_sort(context);
    inc_ref(context, sort_to_ast(context, boolSort));

    long numeralSort;
    if (useIntegers) {
      numeralSort = mk_int_sort(context);
    } else {
      numeralSort = mk_real_sort(context);
    }
    inc_ref(context, sort_to_ast(context, numeralSort));

    // create logger for variables and set initial options in this logger,
    // note: logger for the solvers are created later,
    // they will not contain variable-declaration!
    Z3SmtLogger smtLogger = new Z3SmtLogger(context, config);

    // this options should match the option set above!
    smtLogger.logOption("model", "true");
    smtLogger.logOption("proof", "true");

    // mathsat wants those 2 flags, they are ignored by other solvers
//    smtLogger.logOption("produce-models", "true");
//    smtLogger.logOption("produce-interpolants", "true");
//    smtLogger.logBracket("set-logic QF_UFLRA");


    Z3FormulaCreator creator = new Z3FormulaCreator(context, boolSort, numeralSort, smtLogger);

    // Create managers
    Z3UnsafeFormulaManager unsafeManager = new Z3UnsafeFormulaManager(creator);
    Z3FunctionFormulaManager functionTheory = new Z3FunctionFormulaManager(creator, unsafeManager, smtLogger);
    Z3BooleanFormulaManager booleanTheory = new Z3BooleanFormulaManager(creator);
    Z3RationalFormulaManager rationalTheory = new Z3RationalFormulaManager(creator, functionTheory);
    Z3BitvectorFormulaManager bitvectorTheory = new Z3BitvectorFormulaManager(creator);

    Z3FormulaManager instance = new Z3FormulaManager(
        unsafeManager, functionTheory, booleanTheory,
        rationalTheory, bitvectorTheory, smtLogger, config);
    return instance;
  }

  @Override
  public BooleanFormula parse(String str) throws IllegalArgumentException {

    // TODO do we need sorts or decls?
    // the context should know them already,
    // TODO check this
    long[] sort_symbols = new long[0];
    long[] sorts = new long[0];
    long[] decl_symbols = new long[0];
    long[] decls = new long[0];

    long e = parse_smtlib2_string(z3context, str, sort_symbols, sorts, decl_symbols, decls);

    return encapsulate(BooleanFormula.class, e);
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
  public Appender dumpFormula(final Long expr) {
    // Lazy invocation of ast_to_string wrapped in an Appender.
    return Appenders.fromToStringMethod(
        new Object() {

          @Override
          public String toString() {
            return ast_to_string(z3context, expr);
          }
        });
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

  //  @Override
  //  protected void finalize() {
  //    close_log();
  //  }

  /** returns a new logger with a new logfile. */
  public Z3SmtLogger getSmtLogger() {
    return new Z3SmtLogger(z3smtLogger);
  }

}