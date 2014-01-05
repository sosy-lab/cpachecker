/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.smtinterpol.dpll.DPLLEngine;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.ParseEnvironment;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.SMTInterpol;

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

  @Option(description="Double check generated results like interpolants and models whether they are correct")
  private boolean checkResults = false;

  @Option(description="Export solver queries in Smtlib format into a file.")
  private boolean logAllQueries = false;

  @Option(description="Export interpolation queries in Smtlib format into a file.")
  private boolean logInterpolationQueries = false;

  @Option(name="logfile", description="Export solver queries in Smtlib format into a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path smtLogfile = Paths.get("smtinterpol.%03d.smt2");

  /** this is a counter to get distinct logfiles for distinct environments. */
  private static int logfileCounter = 0;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  /** the wrapped Script */
  private final Script script;
  private final Theory theory;

  /** The stack contains a List of Declarations for each levels on the assertion-stack.
   * It is used to declare functions again, if stacklevels are popped. */
  private final List<Collection<Triple<String, Sort[], Sort>>> stack = new ArrayList<>();

  /** This Collection is the toplevel of the stack. */
  private Collection<Triple<String, Sort[], Sort>> currentDeclarations;

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public SmtInterpolEnvironment(Configuration config, Logics pLogic,
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = checkNotNull(pShutdownNotifier);

    final SMTInterpol smtInterpol = new SMTInterpol(createLog4jLogger(logger));

    if (logAllQueries && smtLogfile != null) {
      script = createLoggingWrapper(smtInterpol);
    } else {
      script = smtInterpol;
    }

    try {
      script.setOption(":produce-interpolants", true);
      script.setOption(":produce-models", true);
      if (checkResults) {
        script.setOption(":interpolant-check-mode", true);
        script.setOption(":unsat-core-check-mode", true);
        script.setOption(":model-check-mode", true);
      }
      script.setLogic(pLogic);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }

    theory = smtInterpol.getTheory();

    shutdownNotifier.registerAndCheckImmediately(new ShutdownNotifier.ShutdownRequestListener() {
        @Override
        public void shutdownRequested(String pReason) {
          DPLLEngine engine = smtInterpol.getEngine();
          if (engine != null) {
            engine.setCompleteness(DPLLEngine.INCOMPLETE_TIMEOUT);
            engine.stop();
          }
        }
      });
  }

  private Script createLoggingWrapper(SMTInterpol smtInterpol) {
    String filename = getFilename(smtLogfile);
    try {
      // create a thin wrapper around Benchmark,
      // this allows to write most formulas of the solver to outputfile
      return new LoggingScript(smtInterpol, filename, true);
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
        // Always log at SEVERE because it is a ERROR message (see above).
        ourLogger.log(Level.SEVERE,
            pArg0.getLoggerName(),
            pArg0.getLevel(),
            "output:",
            pArg0.getRenderedMessage());

        org.apache.log4j.spi.ThrowableInformation throwable = pArg0.getThrowableInformation();
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

  SmtInterpolInterpolatingProver getInterpolator(SmtInterpolFormulaManager mgr) {
    if (logInterpolationQueries && smtLogfile != null) {
      String logfile = getFilename(smtLogfile);

      try {
        PrintWriter out = new PrintWriter(Files.openOutputFile(Paths.get(logfile)));

        out.println("(set-option :produce-interpolants true)");
        out.println("(set-option :produce-models true)");
        if (checkResults) {
          out.println("(set-option :interpolant-check-mode true)");
          out.println("(set-option :unsat-core-check-mode true)");
          out.println("(set-option :model-check-mode true)");
        }

        out.println("(set-logic " + theory.getLogic().name() + ")");
        return new LoggingSmtInterpolInterpolatingProver(mgr, out);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write interpolation query to file");
      }
    }

    return new SmtInterpolInterpolatingProver(mgr);
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

  public void setOption(String opt, Object value) {
    try {
      script.setOption(opt, value);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function declares a new functionSymbol, that has a given (result-) sort.
   * The params for the functionSymbol also have sorts.
   * If you want to declare a new variable, i.e. "X", paramSorts is an empty array. */
  public void declareFun(String fun, Sort[] paramSorts, Sort resultSort) {
    declareFun(fun, paramSorts, resultSort, true);
  }

  /** This function declares a function.
   * It is possible to check, if the function was declared before.
   * If both ('check' and 'declared before') are true, nothing is done. */
  private void declareFun(String fun, Sort[] paramSorts, Sort resultSort, boolean check) {
    if (check) {
      FunctionSymbol fsym = theory.getFunction(fun, paramSorts);

      if (fsym == null) {
        declareFun(fun, paramSorts, resultSort, false);
      } else {
        if (!fsym.getReturnSort().equals(resultSort)) {
          throw new SMTLIBException("Function " + fun + " is already declared with different definition");
        }
      }

    } else {
      script.declareFun(fun, paramSorts, resultSort);
      if (currentDeclarations != null) {
        currentDeclarations.add(Triple.of(fun, paramSorts, resultSort));
      }
    }
  }

  public void push(int levels) {
    try {
      script.push(levels);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }

    for (int i = 0; i < levels; i++) {
      currentDeclarations = new ArrayList<>();
      stack.add(currentDeclarations);
    }
  }

  /** This function pops levels from the assertion-stack.
   * It also declares popped functions on the lower level. */
  public void pop(int levels) {
    assert stack.size() >= levels : "not enough levels to remove";
    try {
     // for (int i=0;i<levels;i++) script.pop(1); // for old version of SmtInterpol
      script.pop(levels);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }

    if (stack.size() - levels > 0) {
      currentDeclarations = stack.get(stack.size() - levels - 1);
    } else {
      currentDeclarations = null;
    }

    for (int i = 0; i < levels; i++) {
      final Collection<Triple<String, Sort[], Sort>> topDecl = stack.remove(stack.size() - 1);

      for (Triple<String, Sort[], Sort> function : topDecl) {
        final String fun = function.getFirst();
        final Sort[] paramSorts = function.getSecond();
        final Sort resultSort = function.getThird();
        declareFun(fun, paramSorts, resultSort, false);
      }
    }
  }

  /** This function adds the term on top of the stack. */
  public void assertTerm(Term term) {
    assert stack.size() > 0 : "assertions should be on higher levels";
    try {
      script.assertTerm(term);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  public boolean checkSat() throws InterruptedException {
    try {
      // We actually terminate SmtInterpol during the analysis
      // by using a shutdown listener. However, SmtInterpol resets the
      // mStopEngine flag in DPLLEngine before starting to solve,
      // so we check here, too.
      shutdownNotifier.shutdownIfNecessary();

      LBool result = script.checkSat();
      switch (result) {
      case SAT:
        return true;
      case UNSAT:
        return false;
      default:
        shutdownNotifier.shutdownIfNecessary();
        throw new SMTLIBException("checkSat returned " + result);
      }
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Iterable<Term[]> checkAllSat(Term[] importantPredicates) throws InterruptedException {
    try {
      // We actually terminate SmtInterpol during the analysis
      // by using a shutdown listener. However, SmtInterpol resets the
      // mStopEngine flag in DPLLEngine before starting to solve,
      // so we check here, too.
      shutdownNotifier.shutdownIfNecessary();

      return script.checkAllsat(importantPredicates);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function returns a map,
   * that contains assignments term->term for all terms in terms. */
  public Map<Term, Term> getValue(Term[] terms) {
    try {
      return script.getValue(terms);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Object getInfo(String info) {
    return script.getInfo(info);
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

  public Term annotate(Term t, Annotation... annotations) {
    try {
      return script.annotate(t, annotations);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term numeral(BigInteger num) {
    try {
      return script.numeral(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term numeral(String num) {
    try {
      return script.numeral(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term decimal(String num) {
    try {
      return script.decimal(num);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

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

  /** This function returns a list of interpolants for the partitions.
   * Each partition must be a named term or a conjunction of named terms.
   * There should be (n-1) interpolants for n partitions. */
  public Term[] getInterpolants(Term[] partition) {
    assert stack.size() > 0 : "interpolants should be on higher levels";
    try {
      return script.getInterpolants(partition);
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
