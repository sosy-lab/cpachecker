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
package org.sosy_lab.cpachecker.util.predicates.princess;

import ap.SimpleAPI;
import ap.basetypes.IdealInt;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.IIntLit;
import ap.parser.ITerm;
import ap.parser.ITermITE;
import scala.Enumeration.Value;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** This is a Wrapper around Princess.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "princess.smt2").
 */
@Options(prefix="cpa.predicate.princess")
class PrincessEnvironment {

  /**
   * Enum listing possible types for Princess.
   */
  static enum Type {
    BOOL("Bool"),
    INT("Int");
    // TODO does Princess support more types?
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

  public class FunctionType {

    private final IFunction funcDecl;
    private final Type resultType;
    private final Type[] args;

    FunctionType(IFunction funcDecl, Type resultType, Type[] args) {
      this.funcDecl = funcDecl;
      this.resultType = resultType;
      this.args = args;
    }

    public IFunction getFuncDecl() { return funcDecl; }
    public Type getResultType() { return resultType; }
    public Type[] getArgs() { return args; }

  }

  /** cache for variables, because they do not implement equals() and hashCode(),
   * so we need to have the same objects. */
  private final Map<String, IExpression> variablesCache = new HashMap<>();
  private final Map<String, FunctionType> functionsCache = new HashMap<>();

  private final Map<IFunction, FunctionType> declaredFunctions = new HashMap<>();

  @Option(description="Export solver queries in Smtlib format into a file.")
  private boolean logAllQueries = false;

  @Option(name="logfile", description="Export solver queries in Smtlib format into a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path smtLogfile = Paths.get("princess.%03d.smt2");

  /** this is a counter to get distinct logfiles for distinct environments. */
  private static int logfileCounter = 0;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  /** the wrapped api */
  private final SimpleAPI api;

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public PrincessEnvironment(Configuration config, final LogManager pLogger,
                             final ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    if (logAllQueries && smtLogfile != null) {
      api = SimpleAPI.spawnWithLogNoSanitise(getFilename(smtLogfile));
    } else {
      api = SimpleAPI.spawnNoSanitise();
    }
    // we do not use 'sanitise', because variable-names contain special chars like "@" and ":"

    api.setConstructProofs(true); // needed for interpolation
  }

  /**  This function creates a filename with following scheme:
       first filename is unchanged, then a number is appended */
  private String getFilename(final Path oldFilename) {
    String filename = oldFilename.toAbsolutePath().getPath();
    return String.format(filename, logfileCounter++);
  }

  public List<IExpression> parseStringToTerms(String s) {
    throw new UnsupportedOperationException(); // todo: implement this
  }

  public void push(int levels) {
    for (int i = 0; i < levels; i++) {
      api.push();
    }
  }

  /** This function pops levels from the assertion-stack. */
  public void pop(int levels) {
    for (int i = 0; i < levels; i++) {
      api.pop();
    }
  }

  /** This function adds the term on top of the stack. */
  public void assertTerm(IFormula booleanFormula) {
    api.addAssertion(booleanFormula);
  }

  /** This function sets a partition number for all the term,
   *  that are asserted  after calling this method, until a new partition number is set. */
  public void assertTermInPartition(IFormula booleanFormula, int index) {
    // set partition number and add formula
    api.setPartitionNumber(index);
    api.addAssertion(booleanFormula);

    // reset partition number to magic number -1, that represents formulae belonging to all partitions.
    api.setPartitionNumber(-1);
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  public boolean checkSat() throws InterruptedException {
    final Value result = api.checkSat(true);
    if (result == SimpleAPI.ProverStatus$.MODULE$.Sat()) {
      return true;
    } else if (result == SimpleAPI.ProverStatus$.MODULE$.Unsat()) {
      return false;
    } else {
      throw new AssertionError("checkSat returned " + result);
    }
  }

  public SimpleAPI.PartialModel getModel() {
    return api.partialModel();
  }

  /** performs a sat-check, that produces a new model
   * TODO check when to stop? */
  public boolean hasNextModel() {
    return api.nextModel(true) == SimpleAPI.ProverStatus$.MODULE$.Sat();
  }

  public IExpression makeVariable(Type type, String varname) {
    // TODO is type important for caching?
    if (variablesCache.containsKey(varname)) {
      return variablesCache.get(varname);
    } else {
      final IExpression var = makeVariable0(type, varname);
      variablesCache.put(varname, var);
      return var;
    }
  }

  private IExpression makeVariable0(Type type, String varname) {
    switch (type) {
      case BOOL:
        return api.createBooleanVariable(varname);
      case INT:
        return api.createConstant(varname);
      default:
        throw new AssertionError("unsupported type: " + type);
    }
  }

  /** This function declares a new functionSymbol, that has a given number of params.
   * Princess has no support for typed params, only their number is important. */
  public FunctionType declareFun(String name, Type resultType, Type[] args) {
    if (functionsCache.containsKey(name)) {
      return functionsCache.get(name);
    } else {
      final FunctionType type = declareFun0(name, resultType, args);
      functionsCache.put(name, type);
      return type;
    }
  }

  /** This function declares a new functionSymbol, that has a given number of params.
   * Princess has no support for typed params, only their number is important. */
  private FunctionType declareFun0(String name, Type resultType, Type[] args) {
    IFunction funcDecl = api.createFunction(name, args.length);
    FunctionType type = new FunctionType(funcDecl, resultType, args);
    declaredFunctions.put(funcDecl, type);
    return type;
  }

  public FunctionType getFunctionDeclaration(IFunction f) {
    return declaredFunctions.get(f);
  }

  public IExpression makeFunction(IFunction funcDecl, Type resultType, List<IExpression> args) {
    final ArrayBuffer<ITerm> argsBuf = new ArrayBuffer<>();
    for (IExpression arg : args) {
      final ITerm termArg;
      if (arg instanceof IFormula) { // boolean term -> build ITE(t,0,1), TODO why not ITE(t,1,0) ??
        termArg = new ITermITE((IFormula)arg, new IIntLit(IdealInt.apply(0)), new IIntLit(IdealInt.apply(1)));
      } else {
        termArg = (ITerm) arg;
      }
      argsBuf.$plus$eq(termArg);
    }

    final ITerm t = new IFunApp(funcDecl, argsBuf.toSeq());

    switch (resultType) {
      case BOOL:
        return t;
      case INT:
        return t;
      default:
        throw new AssertionError("unknown resulttype");
    }
  }

  /** returns a number of type INT or REAL */
  public ITerm numeral(BigInteger num) {
    return new IIntLit(IdealInt.apply(num.toString()));
  }

  /** returns a number of type INT or REAL */
  public ITerm numeral(String num) {
    return new IIntLit(IdealInt.apply(num));
  }

  /** returns a number of type REAL */
  public ITerm decimal(String num) {
    return new IIntLit(IdealInt.apply(num));
  }

  /** returns a number of type REAL */
  public ITerm decimal(BigDecimal num) {
    return new IIntLit(IdealInt.apply(num.toString()));
  }

  /** This function returns a list of interpolants for the partitions.
   * Each partition contains the indizes of its terms.
   * There will be (n-1) interpolants for n partitions. */
  public List<IFormula> getInterpolants(List<Set<Integer>> partitions) {

    // convert to needed data-structure
    final ArrayBuffer<scala.collection.immutable.Set<Object>> args = new ArrayBuffer<>();
    for (Set<Integer> partition :partitions) {
      final ArrayBuffer<Object> indexes = new ArrayBuffer<>();
      for (Integer index : partition)
        indexes.$plus$eq(index);
      args.$plus$eq(indexes.toSet());
    }

    // do the hard work
    final Seq<IFormula> itps = api.getInterpolants(args.toSeq());

    assert itps.length() == partitions.size() - 1 : "There should be (n-1) interpolants for n partitions";

    // convert data-structure back
    final List<IFormula> result = new ArrayList<>(itps.size());
    for (IFormula itp : JavaConversions.asJavaIterable(itps)) {
      result.add(itp);
    }

    return result;
  }

  public String getVersion() {
    return "Princess (unknown version)";
  }

  public void close() {
    logger.log(Level.FINE, "shutting down Princess");
    api.shutDown();
  }
}
