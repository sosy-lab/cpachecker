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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;

import scala.collection.JavaConversions;
import scala.collection.mutable.ArrayBuffer;
import ap.SimpleAPI;
import ap.basetypes.IdealInt;
import ap.parser.IAtom;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.IIntLit;
import ap.parser.ITerm;
import ap.parser.ITermITE;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

/** This is a Wrapper around Princess.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "princess.###.smt2").
 * // TODO logfile is only available as tmpfile in /tmp, perhaps it is not closed?
 * It also manages the "shared variables": each variable is declared for all stacks.
 */
class PrincessEnvironment {

  public static class FunctionType {

    private final IFunction funcDecl;
    private final TermType resultType;
    private final List<TermType> args;

    FunctionType(IFunction funcDecl, TermType resultType, List<TermType> args) {
      this.funcDecl = funcDecl;
      this.resultType = resultType;
      this.args = ImmutableList.copyOf(args);
    }

    public IFunction getFuncDecl() { return funcDecl; }
    public TermType getResultType() { return resultType; }
    public List<TermType> getArgs() { return args; }

  }

  /** cache for variables, because they do not implement equals() and hashCode(),
   * so we need to have the same objects. */
  private final Map<String, IFormula> boolVariablesCache = new HashMap<>();
  private final Map<String, ITerm> intVariablesCache = new HashMap<>();
  private final Map<String, FunctionType> functionsCache = new HashMap<>();

  private final Map<IFunction, FunctionType> declaredFunctions = new HashMap<>();

  // order is important for abbreviations, because a abbreviation might depend on another one.
  private final List<Pair<IFormula, IFormula>> abbrevFormulas = new ArrayList<>();
  // for faster lookup, key: abbreviation, entry: long formula.
  private final BiMap<IFormula, IFormula> abbrevFormulasMap = HashBiMap.create();

  private final @Nullable PathCounterTemplate basicLogfile;

  /** formulas can be simplified through replacing them with an abbrev-formula. */
  // TODO do we have to check, that no other symbol equals an abbreviation-symbol?
  private static final String ABBREV = "ABBREV_";
  private static final UniqueIdGenerator abbrevIndex = new UniqueIdGenerator();

  /** the wrapped api is the first created api.
   * It will never be used outside of this class and never be closed.
   * If a variable is declared, it is declared in the first api, then copied into all registered apis.
   * Each api has its own stack for formulas. */
  private final SimpleAPI api;
  private final List<SymbolTrackingPrincessStack> registeredStacks = new ArrayList<>();

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public PrincessEnvironment(Configuration config, final LogManager pLogger,
      final PathCounterTemplate pBasicLogfile) {
    basicLogfile = pBasicLogfile;
    api = getNewApi(false); // this api is only used local in this environment, no need for interpolation
  }


  /** This method returns a new stack, that is registered in this environment.
   * All variables are shared in all registered stacks. */
  PrincessStack getNewStack(boolean useForInterpolation) {
    SimpleAPI newApi = getNewApi(useForInterpolation);
    SymbolTrackingPrincessStack stack = new SymbolTrackingPrincessStack(this, newApi);

    // add all symbols, that are available until now
    for (IFormula s : boolVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (ITerm s : intVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (FunctionType s : functionsCache.values()) {
      stack.addSymbol(s.funcDecl);
    }
    for (Pair<IFormula, IFormula> abbrev : abbrevFormulas) {
      stack.addAbbrev(abbrev.getFirst(), abbrev.getSecond());
    }
    registeredStacks.add(stack);
    return stack;
  }

  private SimpleAPI getNewApi(boolean useForInterpolation) {
    final SimpleAPI newApi;
    if (basicLogfile != null) {
      newApi = SimpleAPI.spawnWithLogNoSanitise(basicLogfile.getFreshPath().getAbsolutePath());
    } else {
      newApi = SimpleAPI.spawnNoSanitise();
    }
    // we do not use 'sanitise', because variable-names contain special chars like "@" and ":"

    if (useForInterpolation) {
      newApi.setConstructProofs(true); // needed for interpolation
    }
    return newApi;
  }

  void unregisterStack(SymbolTrackingPrincessStack stack) {
    assert registeredStacks.contains(stack) : "cannot remove stack, it is not registered";
    registeredStacks.remove(stack);
  }

  public List<IExpression> parseStringToTerms(String s) {
    throw new UnsupportedOperationException(); // todo: implement this
  }

  public Appender dumpFormula(final IExpression formula) {
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        Set<IExpression> declaredFunctions = PrincessUtil.getVarsAndUIFs(Collections.singleton(formula));

        for (IExpression var : declaredFunctions) {
          out.append("(declare-fun ");
          out.append(getName(var));

          // function parameters
          out.append(" (");
          if (var instanceof IFunApp) {
            IFunApp function = (IFunApp) var;
            Iterator<ITerm> args = JavaConversions.asJavaIterable(function.args()).iterator();
            while (args.hasNext()) {
              args.next();
              // Princess does only support IntegerFormulas in UIFs we don't need
              // to check the type here separately
              if (args.hasNext()) {
                out.append("Int ");
              } else {
                out.append("Int");
              }
            }
          }

          out.append(") ");
          out.append(getType(var));
          out.append(")\n");
        }

        out.append("(assert ");
        out.append(formula.toString());
        out.append(")");
      }
    };
  }

  private String getName(IExpression var) {
    if (var instanceof IAtom) {
      return ((IAtom) var).pred().name();
    } else if (var instanceof IConstant) {
      return ((IConstant)var).toString();
    } else if (var instanceof IFunApp) {
      String fullStr = ((IFunApp)var).fun().toString();
      return fullStr.substring(0, fullStr.indexOf("/"));
    }

    throw new IllegalArgumentException("The given parameter is no variable or function");
  }

  private String getType(IExpression var) {
    if (var instanceof IFormula) {
      return "Bool";

      // functions are included here, they cannot be handled separate for princess
    } else if (var instanceof ITerm) {
      return "Int";
    }

    throw new IllegalArgumentException("The given parameter is no variable or function");
  }

  public IExpression makeVariable(TermType type, String varname) {
    switch (type) {

      case Boolean: {
        if (boolVariablesCache.containsKey(varname)) {
          return boolVariablesCache.get(varname);
        } else {
          IFormula var = api.createBooleanVariable(varname);
          for (SymbolTrackingPrincessStack stack : registeredStacks) {
            stack.addSymbol(var);
          }
          boolVariablesCache.put(varname, var);
          return var;
        }
      }

      case Integer: {
        if (intVariablesCache.containsKey(varname)) {
          return intVariablesCache.get(varname);
        } else {
          ITerm var = api.createConstant(varname);
          for (SymbolTrackingPrincessStack stack : registeredStacks) {
            stack.addSymbol(var);
          }
          intVariablesCache.put(varname, var);
          return var;
        }
      }

      default:
        throw new AssertionError("unsupported type: " + type);
    }
  }

  /** This function declares a new functionSymbol, that has a given number of params.
   * Princess has no support for typed params, only their number is important. */
  public FunctionType declareFun(String name, TermType resultType, List<TermType> args) {
    if (functionsCache.containsKey(name)) {
      FunctionType function = functionsCache.get(name);
      assert function.getResultType() == resultType;
      assert function.getArgs().equals(args);
      return function;
    } else {
      final FunctionType type = declareFun0(name, resultType, args);
      functionsCache.put(name, type);
      return type;
    }
  }

  /** This function declares a new functionSymbol, that has a given number of params.
   * Princess has no support for typed params, only their number is important. */
  private FunctionType declareFun0(String name, TermType resultType, List<TermType> args) {
    IFunction funcDecl = api.createFunction(name, args.size());
    for (SymbolTrackingPrincessStack stack : registeredStacks) {
       stack.addSymbol(funcDecl);
    }
    FunctionType type = new FunctionType(funcDecl, resultType, args);
    declaredFunctions.put(funcDecl, type);
    return type;
  }

  public FunctionType getFunctionDeclaration(IFunction f) {
    return declaredFunctions.get(f);
  }

  public IExpression makeFunction(IFunction funcDecl, TermType resultType, List<IExpression> args) {
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
      case Boolean:
        return t;
      case Integer:
        return t;
      default:
        throw new AssertionError("unknown resulttype");
    }
  }

  /** create a short replacement/abbreviation function for a long formula. */
  public IFormula abbrev(IFormula longFormula) {
    if (abbrevFormulasMap.inverse().containsKey(longFormula)) {
      return abbrevFormulasMap.inverse().get(longFormula);
    } else {
      final String abbrevName = ABBREV + abbrevIndex.getFreshId();
      final IFormula abbrev = api.abbrev(longFormula, abbrevName);
      abbrevFormulas.add(Pair.of(abbrev, longFormula));
      abbrevFormulasMap.put(abbrev, longFormula);
      for (SymbolTrackingPrincessStack stack : registeredStacks) {
        stack.addAbbrev(abbrev, longFormula);
      }
      return abbrev;
    }
  }

  public String getVersion() {
    return "Princess (unknown version)";
  }
}
