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

import static com.google.common.base.Preconditions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.LoggingScript;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.ReasonUnknown;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.logic.simplification.SimplifyDDA;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.ParseEnvironment;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.SMTInterpol;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.TerminationRequest;

/** This is a Wrapper around SmtInterpol.
 * It guarantees the stack-behavior of function-declarations towards the SmtSolver,
 * so functions remain declared, if levels are popped.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "smtinterpol.smt2").
 */
@Options(prefix="cpa.predicate.solver.smtinterpol")
class SmtInterpolEnvironment {

  @Option(secure=true, description="Double check generated results like interpolants and models whether they are correct")
  private boolean checkResults = false;

  private final @Nullable PathCounterTemplate smtLogfile;

  @Option(secure=true, description = "List of further options which will be set to true for SMTInterpol in addition to the default options. "
      + "Format is 'option1,option2,option3'")
  private List<String> furtherOptions = ImmutableList.of();

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  /** the wrapped Script */
  private final Script script;
  private final Theory theory;

  /** The current depth of the stack in the solver. */
  private int stackDepth = 0;

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public SmtInterpolEnvironment(Configuration config,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier,
      @Nullable PathCounterTemplate pSmtLogfile, long randomSeed)
          throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    smtLogfile = pSmtLogfile;

    final SMTInterpol smtInterpol = new SMTInterpol(createLog4jLogger(logger),
        new TerminationRequest() {
          @Override
          public boolean isTerminationRequested() {
            return pShutdownNotifier.shouldShutdown();
          }
        });

    if (smtLogfile != null) {
      script = createLoggingWrapper(smtInterpol);
    } else {
      script = smtInterpol;
    }

    try {
      script.setOption(":random-seed", randomSeed);
      script.setOption(":produce-interpolants", true);
      script.setOption(":produce-models", true);
      script.setOption(":produce-unsat-cores", true);
      if (checkResults) {
        script.setOption(":interpolant-check-mode", true);
        script.setOption(":unsat-core-check-mode", true);
        script.setOption(":model-check-mode", true);
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

    theory = smtInterpol.getTheory();
  }

  private Script createLoggingWrapper(SMTInterpol smtInterpol) {
    String filename = smtLogfile.getFreshPath().toAbsolutePath().toString();
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

  SmtInterpolInterpolatingProver getInterpolator(SmtInterpolFormulaManager mgr) {
    if (smtLogfile != null) {
      Path logfile = smtLogfile.getFreshPath();

      try {
        PrintWriter out = new PrintWriter(Files.openOutputFile(logfile));

        out.println("(set-option :random-seed " + script.getOption(":random-seed") + ")");
        out.println("(set-option :produce-interpolants true)");
        out.println("(set-option :produce-models true)");
        out.println("(set-option :produce-unsat-cores true)");
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
    checkState(stackDepth == 0,
        "Not allowed to create a new prover environment while solver stack is still non-empty, parallel stacks are not supported.");
    return new SmtInterpolTheoremProver(mgr);
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
    FunctionSymbol fsym = theory.getFunction(fun, paramSorts);

    if (fsym == null) {
      script.declareFun(fun, paramSorts, resultSort);
    } else {
      if (!fsym.getReturnSort().equals(resultSort)) {
        throw new SMTLIBException("Function " + fun + " is already declared with different definition");
      }
    }
  }

  public void push(int levels) {
    checkArgument(levels > 0);
    try {
      script.push(levels);
      stackDepth += levels;
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function pops levels from the assertion-stack.
   * It also declares popped functions on the lower level. */
  public void pop(int levels) {
    checkArgument(levels >= 0);
    checkState(stackDepth >= levels, "not enough levels to remove");
    try {
      script.pop(levels);
      stackDepth -= levels;
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /** This function adds the term on top of the stack. */
  public void assertTerm(Term term) {
    checkState(stackDepth > 0, "assertions should be on higher levels");
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
    checkState(stackDepth > 0, "checkSat should be on higher levels");
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
      case UNKNOWN:
        shutdownNotifier.shutdownIfNecessary();
        Object reason = script.getInfo(":reason-unknown");
        if (!(reason instanceof ReasonUnknown)) {
          throw new SMTLIBException("checkSat returned UNKNOWN with unknown reason " + reason);
        }
        switch ((ReasonUnknown)reason) {
        case MEMOUT:
          // SMTInterpol catches OOM, but we want to have it thrown.
          throw new OutOfMemoryError("Out of memory during SMTInterpol operation");
        default:
          throw new SMTLIBException("checkSat returned UNKNOWN with unexpected reason " + reason);
        }

      default:
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
  public Model getModel() {
    return script.getModel();
  }

  public Object getInfo(String info) {
    return script.getInfo(info);
  }

  public Sort getBooleanSort() {
    return theory.getBooleanSort();
  }

  public Sort getIntegerSort() {
    return theory.getNumericSort();
  }

  public Sort getRealSort() {
    return theory.getRealSort();
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

  public Term term(String funcname, BigInteger[] indices,
      Sort returnSort, Term... params) {
    try {
      return script.term(funcname, indices, returnSort, params);
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

  /** This function returns a list of interpolants for the partitions.
   * Each partition must be a named term or a conjunction of named terms.
   * There should be (n-1) interpolants for n partitions. */
  public Term[] getInterpolants(Term[] partition) {
    checkState(stackDepth > 0, "interpolants should be on higher levels");
    try {
      return script.getInterpolants(partition);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Compute a sequence of interpolants. The nesting array describes the
   * start of the subtree for tree interpolants. For inductive sequences of
   * interpolants use a nesting array completely filled with 0.
   *
   * Example:
   *
   * A  D
   * |  |
   * B  E
   * | /
   * C
   * |
   * F  H
   * | /
   * G
   *
   * arrayIndex     = [0,1,2,3,4,5,6,7]  // only for demonstration, not needed
   * partition      = [A,B,D,E,C,F,H,G]  // post-order of tree
   * startOfSubTree = [0,0,2,2,0,0,6,0]  // index of left-most leaf of the current element
   *
   * @param partition The array of formulas (post-order of tree).
   *                  This should contain either top-level names or conjunction of top-level names.
   * @param startOfSubtree The start of the subtree containing the formula at this index as root.
   * @return Tree interpolants respecting the nesting relation.
   */
  public Term[] getTreeInterpolants(Term[] partition, int[] startOfSubTree) {
    checkState(stackDepth > 0, "interpolants should be on higher levels");
    try {
      return script.getInterpolants(partition, startOfSubTree);
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term[] getUnsatCore() {
    checkState(stackDepth > 0, "unsat core should be on higher levels");
    try {
      return script.getUnsatCore();
    } catch (SMTLIBException e) {
      throw new AssertionError(e);
    }
  }

  public Term simplify(Term input) {
    try {
      SimplifyDDA s = new SimplifyDDA(script, true);
      return s.getSimplifiedTerm(input);
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
