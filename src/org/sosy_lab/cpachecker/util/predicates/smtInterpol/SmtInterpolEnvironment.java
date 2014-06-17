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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.ParseEnvironment;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.SMTInterpol;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.TerminationRequest;

/** This is a Wrapper around SmtInterpol.
 * It guarantees the stack-behavior of function-declarations towards the SmtSolver,
 * so functions remain declared, if levels are popped.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "smtinterpol.smt2").
 */
@Options(prefix="cpa.predicate.smtinterpol")
class SmtInterpolEnvironment {

  /**
   * Enum listing possible types for SmtInterpol.
   */
  static enum Type {
    BOOL("Bool"),
    INT("Int"),
    REAL("Real");
    // TODO more types?
    // TODO merge enum with ModelTypes?

    private final String name;

    private Type(String s) {
      name = s;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private class SymbolLevel {
    List<Triple<String, Sort[], Sort>> functionSymbols = new ArrayList<>();

    void add(String fun, Sort[] paramSorts, Sort resultSort) {
      functionSymbols.add(Triple.of(fun, paramSorts, resultSort));
    }

    /**  add higher level to current level, we keep the order of creating symbols. */
    void mergeWithHigher(SymbolLevel other) {
      this.functionSymbols.addAll(other.functionSymbols);
    }
  }

  @Option(description="Double check generated results like interpolants and models whether they are correct")
  private boolean checkResults = false;

  @Option(description="Export solver queries in Smtlib format into a file.")
  private boolean logAllQueries = false;

  @Option(description="Export interpolation queries in Smtlib format into a file.")
  private boolean logInterpolationQueries = false;

  @Option(name="logfile", description="Export solver queries in Smtlib format into a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path smtLogfile = Paths.get("smtinterpol.%03d.smt2");

  @Option(description = "List of further options which will be set to true for SMTInterpol in addition to the default options. "
      + "Format is 'option1,option2,option3'")
  private List<String> furtherOptions = ImmutableList.of();

  /** this is a counter to get distinct logfiles for distinct environments. */
  private static int logfileCounter = 0;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  /** The wrapped script is the first created Script.
   * It will never be used outside of this class.
   * SMTInterpol shares one Theory across all instances of the Scripts, that are created with createNewScript().
   * If a Symbol is declared (in the Theory), then it is automatically available in all Scripts.
   * We have to maintain a stack of Symbols, because Symbols might be deleted through pop(). */
  private final Script script;
  private final SMTInterpol smtInterpol;
  private final Theory theory;
  private final Deque<SymbolLevel> symbolStack = new ArrayDeque<>();
  final Map<String,Object> options;

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public SmtInterpolEnvironment(Configuration config,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    smtInterpol = new SMTInterpol(createLog4jLogger(logger),
        new TerminationRequest() {
          @Override
          public boolean isTerminationRequested() {
            return pShutdownNotifier.shouldShutdown();
          }
        });

    if (logAllQueries && smtLogfile != null) {
      script = createLoggingWrapper(smtInterpol);
    } else {
      script = smtInterpol;
    }

    // set common options, important: shared instances need the same options!
    options = new HashMap<>();
    options.put(":produce-models", true);
    options.put(":produce-interpolants", true);
    options.put(":produce-unsat-cores", true);
    if (checkResults) {
      options.put(":interpolant-check-mode", true);
      options.put(":unsat-core-check-mode", true);
      options.put(":model-check-mode", true);
    }

    try {
      for (String key: options.keySet()) {
        script.setOption(key, options.get(key));
      }
      script.setLogic(Logics.QF_UFLIRA);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }

    for (String option : furtherOptions) {
      try {
        script.setOption(":" + option, true);
      } catch (SMTLIBException | UnsupportedOperationException e) {
        throw new InvalidConfigurationException("Invalid option \"" + option + "\" for SMTInterpol.", e);
      }
    }

    // we do not set any options in the main-script,
    // because this script is only used local to track all symbols.
    // there is no need of having a stack or checking for SAT in the main-script.

    theory = smtInterpol.getTheory();
  }

  public Script getNewScript() {

    // we use the copy-constructor to have the same theory in all stacks.
    final Map<String, Object> newOptions = Maps.newHashMap(options);
    for (String key : furtherOptions) {
      newOptions.put(key, true);
    }
    final SMTInterpol newSmtInterpol = new SMTInterpol(smtInterpol, newOptions);

    final Script newScript;
    if (logAllQueries && smtLogfile != null) {
      newScript = createLoggingWrapper(newSmtInterpol);
    } else {
      newScript = newSmtInterpol;
    }

    assert newSmtInterpol.getTheory() == theory : "new stack must have same theory, " +
            "otherwise we can not use the same terms in several distinct stacks.";
    // we return both, the (optional) wrapper-script and the original smtInterpol-script.
    return newScript;
  }

  private Script createLoggingWrapper(SMTInterpol smtInterpol) {
    String filename = getFilename(smtLogfile);
    try {
      // create a thin wrapper around Benchmark,
      // this allows to write most formulas of the solver to outputfile
      return new LoggingScript(smtInterpol, filename, true, true);
    } catch (FileNotFoundException e) {
      logger.logUserException(Level.WARNING, e, "Coud not open log file for SMTInterpol queries");
      // go on without logging
      return smtInterpol;
    }
  }

  private static org.apache.log4j.Logger createLog4jLogger(final LogManager ourLogger) {
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("SMTInterpol");
    // levels: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
    // WARN is too noisy.
    logger.setLevel(org.apache.log4j.Level.ERROR);
    logger.addAppender(new org.apache.log4j.AppenderSkeleton() {

      @Override
      public boolean requiresLayout() {
        return false;
      }

      @Override
      public void close() {}

      @Override
      protected void append(org.apache.log4j.spi.LoggingEvent pArg0) {
        // SMTInterpol has serveral "catch (Throwable t) { log(t); }",
        // which is very ugly because it also catches errors like OutOfMemoryError
        // and ThreadDeath.
        // We do a similarly ugly thing and rethrow such exceptions here
        // (at least for errors and runtime exceptions).
        org.apache.log4j.spi.ThrowableInformation throwable = pArg0.getThrowableInformation();
        if (throwable != null) {
          Throwables.propagateIfPossible(throwable.getThrowable());
        }

        // Always log at SEVERE because it is a ERROR message (see above).
        ourLogger.log(Level.SEVERE,
            pArg0.getLoggerName(),
            pArg0.getLevel(),
            "output:",
            pArg0.getRenderedMessage());

        if (throwable != null) {
          ourLogger.logException(Level.SEVERE, throwable.getThrowable(),
              pArg0.getLoggerName() + " exception");
        }
      }
    });
    return logger;
  }

  /**
   * Be careful when accessing the Theory directly,
   * because operations on it won't be caught by the LoggingScript.
   * It is ok to create terms using the Theory, not to define them or call checkSat.
   */
  Theory getTheory() {
    return theory;
  }

  /**  This function creates a filename with following scheme:
       first filename is unchanged, then a number is appended */
  private String getFilename(final Path oldFilename) {
    String filename = oldFilename.toAbsolutePath().getPath();
    return String.format(filename, logfileCounter++);
  }

  SmtInterpolInterpolatingProver createInterpolator(SmtInterpolFormulaManager mgr) {
    if (logInterpolationQueries && smtLogfile != null) {
      String logfile = getFilename(smtLogfile);

      try {
        PrintWriter out = new PrintWriter(Files.openOutputFile(Paths.get(logfile)));

        out.println("(set-option :produce-interpolants true)");
        out.println("(set-option :produce-models true)");
        out.println("(set-option :produce-unsat-cores true)"); // TODO unsat-cores needed?
        if (checkResults) {
          out.println("(set-option :interpolant-check-mode true)");
          out.println("(set-option :unsat-core-check-mode true)");
          out.println("(set-option :model-check-mode true)");
        }

        out.println("(set-logic " + theory.getLogic().name() + ")");
        return new LoggingSmtInterpolInterpolatingProver(mgr, shutdownNotifier, out);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write interpolation query to file");
      }
    }

    return new SmtInterpolInterpolatingProver(mgr, shutdownNotifier);
  }

  SmtInterpolTheoremProver createProver(SmtInterpolFormulaManager mgr) {
    return new SmtInterpolTheoremProver(mgr, shutdownNotifier);
  }

  /** Parse a String to Terms and Declarations.
   * The String may contain terms and function-declarations in SMTLIB2-format.
   * Use Prefix-notation! */
  public List<Term> parseStringToTerms(String s) {
    FormulaCollectionScript parseScript = new FormulaCollectionScript(script, theory);
    ParseEnvironment parseEnv = new ParseEnvironment(parseScript) {
      @Override
      public void printError(String pMessage) {
        throw new SMTLIBException(pMessage);
      }

      @Override
      public void printSuccess() { }
    };

    try {
      parseEnv.parseStream(new StringReader(s), "<stdin>");
    } catch (SMTLIBException e) {
      throw new IllegalArgumentException("Could not parse term:" + e.getMessage(), e);
    }

    return parseScript.getAssertedTerms();
  }

  /** This function declares a new functionSymbol, that has a given (result-) sort.
   * The params for the functionSymbol also have sorts.
   * If you want to declare a new variable, i.e. "X", paramSorts is an empty array. */
  public void declareFun(String fun, Sort[] paramSorts, Sort resultSort) {
    if (theory.getFunction(fun, paramSorts) == null) {
      // if symbol is not already existant
      script.declareFun(fun, paramSorts, resultSort);
    }
    if (!symbolStack.isEmpty()) {
      symbolStack.getLast().add(fun, paramSorts, resultSort);
    }
  }

  public void push(int levels) {
    // we have to track symbols on higher levels, because CPAchecker assumes "global" symbols.
    for (int i = 0; i < levels; i++) {
      symbolStack.addLast(new SymbolLevel());
    }
  }

  public void pop(int levels) {
    // we have to recreate symbols on lower levels, because CPAchecker assumes "global" symbols.
    final Deque<SymbolLevel> toAdd = new ArrayDeque<>(levels);
    for (int i = 0; i < levels; i++) {
      toAdd.add(symbolStack.removeLast());
    }
    for (SymbolLevel level : toAdd) {
      for (Triple<String,Sort[],Sort> function : level.functionSymbols) {
        declareFun(function.getFirst(), function.getSecond(), function.getThird());
      }
      if (!symbolStack.isEmpty()) {
        symbolStack.getLast().mergeWithHigher(level);
      }
    }
  }
  /** This function returns the Sort for a Type. */
  public Sort sort(Type type) {
    return sort(type.toString());
  }

  /** This function returns an n-ary sort with given parameters. */
  Sort sort(String sortname, Sort... params) {
    try {
      return script.sort(sortname, params);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term term(String funcname, Term... params) {
    try {
      return script.term(funcname, params);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public TermVariable variable(String varname, Sort sort) {
    try {
      return script.variable(varname, sort);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term quantifier(int quantor, TermVariable[] vars, Term body, Term[]... patterns) {
    try {
      return script.quantifier(quantor, vars, body, patterns);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term let(TermVariable[] pVars, Term[] pValues, Term pBody) {
    try {
      return script.let(pVars, pValues, pBody);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** returns a number of type INT or REAL */
  public Term numeral(BigInteger num) {
    try {
      return script.numeral(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** returns a number of type INT or REAL */
  public Term numeral(String num) {
    try {
      return script.numeral(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** returns a number of type REAL */
  public Term decimal(String num) {
    try {
      return script.decimal(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** returns a number of type REAL */
  public Term decimal(BigDecimal num) {
    try {
      return script.decimal(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term hexadecimal(String hex) {
    try {
      return script.hexadecimal(hex);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term binary(String bin) {
    try {
      return script.binary(bin);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function returns the version of SmtInterpol, for logging. */
  public String getVersion() {
    QuotedObject program = (QuotedObject)script.getInfo(":name");
    QuotedObject version = (QuotedObject)script.getInfo(":version");
    return program.getValue() + " " + version.getValue();
  }
}
