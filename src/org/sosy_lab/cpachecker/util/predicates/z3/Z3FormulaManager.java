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

import java.io.IOException;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.NativeLibraries.OS;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToInt;

import com.google.common.base.Splitter;

@Options(deprecatedPrefix="cpa.predicate.solver.z3",
         prefix="solver.z3")
public class Z3FormulaManager extends AbstractFormulaManager<Long, Long, Long> implements AutoCloseable {

  @Option(secure=true, description = "simplify formulas when they are asserted in a solver.")
  boolean simplifyFormulas = false;

  /** Optimization settings */
  @Option(secure=true, description = "Engine to use for the optimization",
    values = {"basic", "farkas", "symba"})
  String optimizationEngine = "basic";

  @Option(secure=true, description = "Ordering for objectives in the optimization" +
      " context", values = {"lex", "pareto", "box"})
  String objectivePrioritizationMode = "box";

  private final Z3SmtLogger z3smtLogger;

  // Pointer from class is needed to avoid GC claiming this listener.
  private final ShutdownNotifier.ShutdownRequestListener interruptListener;
  private Z3AstMatcher z3astMatcher;
  private final long z3params;
  private final ShutdownNotifier shutdownNotfier;

  private static final String OPT_ENGINE_CONFIG_KEY = "optsmt_engine";
  private static final String OPT_PRIORITY_CONFIG_KEY = "priority";

  @Options(deprecatedPrefix="cpa.predicate.solver.z3",
           prefix="solver.z3")
  public static class ExtraOptions {
    @Option(secure=true, description="Require proofs from SMT solver")
    boolean requireProofs = true;

    @Option(secure=true, description="Activate replayable logging in Z3."
        + " The log can be given as an input to the solver and replayed.")
    @FileOption(Type.OUTPUT_FILE)
    Path log = null;
  }

  private Z3FormulaManager(
      Z3FormulaCreator pFormulaCreator,
      Z3UnsafeFormulaManager pUnsafeManager,
      Z3FunctionFormulaManager pFunctionManager,
      Z3BooleanFormulaManager pBooleanManager,
      Z3IntegerFormulaManager pIntegerManager,
      Z3RationalFormulaManager pRationalManager,
      Z3BitvectorFormulaManager pBitpreciseManager,
      Z3QuantifiedFormulaManager pQuantifiedManager,
      Z3ArrayFormulaManager pArrayManager,
      Z3SmtLogger smtLogger, Configuration config, long pZ3params,
      ShutdownNotifier.ShutdownRequestListener pInterruptListener,
      ShutdownNotifier pShutdownNotifier) throws
        InvalidConfigurationException {

    super(pFormulaCreator, pUnsafeManager, pFunctionManager, pBooleanManager,
        pIntegerManager, pRationalManager, pBitpreciseManager, null, pQuantifiedManager, pArrayManager);

    config.inject(this);
    z3params = pZ3params;
    this.z3smtLogger = smtLogger;
    this.z3astMatcher = new Z3AstMatcher(this);
    interruptListener = pInterruptListener;
    pShutdownNotifier.register(interruptListener);
    shutdownNotfier = pShutdownNotifier;
  }

  public static synchronized Z3FormulaManager create(LogManager logger,
      Configuration config, ShutdownNotifier pShutdownNotifier,
      @Nullable PathCounterTemplate solverLogfile, long randomSeed)
      throws InvalidConfigurationException {
    ExtraOptions extraOptions = new ExtraOptions();
    config.inject(extraOptions);

    if (NativeLibraries.OS.guessOperatingSystem() == OS.WINDOWS) {
      // Z3 itself
      NativeLibraries.loadLibrary("libz3");
    }

    NativeLibraries.loadLibrary("z3j");

    if (extraOptions.log != null) {
      Path absolutePath = extraOptions.log.toAbsolutePath();
      try {
        // Z3 segfaults if it cannot write to the file, thus we write once first
        Files.writeFile(absolutePath, "");

        open_log(absolutePath.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Cannot write Z3 log file");
      }
    }

    long cfg = mk_config();
    set_param_value(cfg, "MODEL", "true");

    if (extraOptions.requireProofs) {
      set_param_value(cfg, "PROOF", "true");
    }
    global_param_set("smt.random_seed", String.valueOf(randomSeed));

    // TODO add some other params, memory-limit?
    final long context = mk_context_rc(cfg);
    ShutdownNotifier.ShutdownRequestListener interruptListener =
        new ShutdownNotifier.ShutdownRequestListener() {
          @Override
          public void shutdownRequested(String reason) {
            interrupt(context);
          }
        };
    del_config(cfg);

    long boolSort = mk_bool_sort(context);
    inc_ref(context, sort_to_ast(context, boolSort));

    long integerSort = mk_int_sort(context);
    inc_ref(context, sort_to_ast(context, integerSort));
    long realSort = mk_real_sort(context);
    inc_ref(context, sort_to_ast(context, realSort));

    // The string representations of Z3s formulas should be in SMTLib2!
    set_ast_print_mode(context, Z3NativeApiConstants.Z3_PRINT_SMTLIB2_COMPLIANT);

    // create logger for variables and set initial options in this logger,
    // note: logger for the solvers are created later,
    // they will not contain variable-declaration!
    Z3SmtLogger smtLogger = new Z3SmtLogger(context, config, solverLogfile);

    // this options should match the option set above!
    smtLogger.logOption("model", "true");
    if (extraOptions.requireProofs) {
      smtLogger.logOption("proof", "true");
    }

    long z3params = mk_params(context);
    params_inc_ref(context, z3params);
    params_set_uint(context, z3params, mk_string_symbol(context, ":random-seed"), 42);
    smtLogger.logOption("random-seed", Integer.toString((int)randomSeed));

    Z3FormulaCreator creator = new Z3FormulaCreator(context, boolSort, integerSort, realSort, smtLogger);

    // Create managers
    Z3UnsafeFormulaManager unsafeManager = new Z3UnsafeFormulaManager(creator);
    Z3FunctionFormulaManager functionTheory = new Z3FunctionFormulaManager(creator, unsafeManager, smtLogger);
    Z3BooleanFormulaManager booleanTheory = new Z3BooleanFormulaManager(creator);
    Z3IntegerFormulaManager integerTheory = new Z3IntegerFormulaManager(creator, functionTheory);
    Z3RationalFormulaManager rationalTheory = new Z3RationalFormulaManager(creator, functionTheory);
    Z3BitvectorFormulaManager bitvectorTheory = new Z3BitvectorFormulaManager(creator);
    Z3QuantifiedFormulaManager quantifierManager = new Z3QuantifiedFormulaManager(creator);
    Z3ArrayFormulaManager arrayManager = new Z3ArrayFormulaManager(creator);

    // Set the custom error handling
    // which will throw java Exception
    // instead of exit(1).
    setInternalErrorHandler(context);
    return new Z3FormulaManager(
        creator,
        unsafeManager, functionTheory, booleanTheory,
        integerTheory, rationalTheory, bitvectorTheory, quantifierManager, arrayManager,
        smtLogger, config, z3params, interruptListener, pShutdownNotifier);
  }

  @Override
  public ProverEnvironment newProverEnvironment(boolean pGenerateModels, boolean pGenerateUnsatCore) {
    return new Z3TheoremProver(this, z3params, shutdownNotfier, pGenerateUnsatCore);
  }

  @Override
  public Z3InterpolatingProver newProverEnvironmentWithInterpolation(boolean pShared) {
    return new Z3InterpolatingProver(this, z3params);
  }

  @Override
  public SmtAstMatcher getSmtAstMatcher() {
    return z3astMatcher;
  }

  @Override
  public OptEnvironment newOptEnvironment() {
    Z3OptProver out = new Z3OptProver(this, shutdownNotfier);
    out.setParam(OPT_ENGINE_CONFIG_KEY, this.optimizationEngine);
    out.setParam(OPT_PRIORITY_CONFIG_KEY, this.objectivePrioritizationMode);
    return out;
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

    long e = parse_smtlib2_string(getEnvironment(), str, sort_symbols, sorts, decl_symbols, decls);

    return encapsulateBooleanFormula(e);
  }

  static long getZ3Expr(Formula pT) {
    if (pT instanceof Z3Formula) {
      return ((Z3Formula)pT).getFormulaInfo();
    }
    throw new IllegalArgumentException("Cannot get the formula info of type " + pT.getClass().getSimpleName() + " in the Solver!");
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
    assert getFormulaCreator().getFormulaType(expr) == FormulaType.BooleanType : "Only BooleanFormulas may be dumped";

    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        String txt = Z3NativeApi.benchmark_to_smtlib_string(getEnvironment(), "dumped-formula", "", "unknown", "", 0, new long[]{}, expr);

        for (String line : Splitter.on('\n').split(txt)) {

          if (line.startsWith("(set-info")
              || line.startsWith(";")
              || line.startsWith("(check")) {
            // ignore
          } else if (line.startsWith("(assert")
              || line.startsWith("(dec")) {
            out.append('\n');
            out.append(line);
          } else {
            // Z3 spans formulas over multiple lines, append to previous line
            out.append(' ');
            out.append(line.trim());
          }
        }
      }
    };
  }

  protected BooleanFormula encapsulateBooleanFormula(long t) {
    return getFormulaCreator().encapsulateBoolean(t);
  }

  /** returns a new logger with a new logfile. */
  Z3SmtLogger getSmtLogger() {
    return z3smtLogger.cloneWithNewLogfile();
  }

  @Override
  public void close() {
    long context = getFormulaCreator().getEnv();
    params_dec_ref(context, z3params);
    del_context(context);
  }
}