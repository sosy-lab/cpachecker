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
package org.sosy_lab.solver.princess;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singleton;
import static org.sosy_lab.solver.princess.PrincessUtil.getVarsAndUIFs;
import static scala.collection.JavaConversions.asJavaIterable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.solver.TermType;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import ap.SimpleAPI;
import ap.basetypes.IdealInt;
import ap.parser.IAtom;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IExpression.BooleanFunApplier;
import ap.parser.IFormula;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.IIntLit;
import ap.parser.ITerm;
import ap.parser.ITermITE;
import ap.parser.SMTLineariser;
import scala.collection.JavaConversions;
import scala.collection.mutable.ArrayBuffer;

/** This is a Wrapper around Princess.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "princess.###.smt2").
 * It also manages the "shared variables": each variable is declared for all stacks.
 */
class PrincessEnvironment {

  /** cache for variables, because they do not implement equals() and hashCode(),
   * so we need to have the same objects. */
  private final Map<String, IFormula> boolVariablesCache = new HashMap<>();
  private final Map<String, ITerm> intVariablesCache = new HashMap<>();

  /** The key of this map is the abbreviation, the value is the full expression.*/
  private final Map<IExpression, IExpression> abbrevCache = new LinkedHashMap<>();
  /** This map is necessary because of the missing equals implementations on princess expressions */
  private final Map<String, IExpression> stringToAbbrev = new HashMap<>();
  private final Map<String, IFunction> functionsCache = new HashMap<>();
  private final Map<IFunction, TermType> functionsReturnTypes = new HashMap<>();

  private final @Nullable PathCounterTemplate basicLogfile;
  private final ShutdownNotifier shutdownNotifier;

  /** the wrapped api is the first created api.
   * It will never be used outside of this class and never be closed.
   * If a variable is declared, it is declared in the first api, then copied into all registered apis.
   * Each api has its own stack for formulas. */
  private final SimpleAPI api;
  private final List<SymbolTrackingPrincessStack> registeredStacks = new ArrayList<>();
  private final List<SymbolTrackingPrincessStack> reusableStacks = new ArrayList<>();
  private final List<SymbolTrackingPrincessStack> allStacks = new ArrayList<>();

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger.
   * @param pShutdownNotifier */
  public PrincessEnvironment(Configuration config, final LogManager pLogger,
      final PathCounterTemplate pBasicLogfile, ShutdownNotifier pShutdownNotifier) {
    basicLogfile = pBasicLogfile;
    shutdownNotifier = pShutdownNotifier;
    api = getNewApi(false); // this api is only used local in this environment, no need for interpolation
  }


  /** This method returns a new stack, that is registered in this environment.
   * All variables are shared in all registered stacks. */
  PrincessStack getNewStack(boolean useForInterpolation) {
    // shortcut if we have a reusable stack
    for (Iterator<SymbolTrackingPrincessStack> it = reusableStacks.iterator(); it.hasNext();) {
      SymbolTrackingPrincessStack stack = it.next();
      if (stack.canBeUsedForInterpolation() == useForInterpolation) {
        registeredStacks.add(stack);
        it.remove();
        return stack;
      }
    }

    // if not we have to create a new one

    SimpleAPI newApi = getNewApi(useForInterpolation);
    SymbolTrackingPrincessStack stack = new SymbolTrackingPrincessStack(this, newApi, useForInterpolation, shutdownNotifier);

    // add all symbols, that are available until now
    for (IFormula s : boolVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (ITerm s : intVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (IFunction s : functionsCache.values()) {
      stack.addSymbol(s);
    }
    for(Entry<IExpression, IExpression> e : abbrevCache.entrySet()) {
      stack.addAbbrev(e.getKey(), e.getValue());
    }
    registeredStacks.add(stack);
    allStacks.add(stack);
    return stack;
  }

  private SimpleAPI getNewApi(boolean useForInterpolation) {
    final SimpleAPI newApi;
    if (basicLogfile != null) {
      Path logPath = basicLogfile.getFreshPath();
      String fileName = logPath.getName();
      String absPath = logPath.getAbsolutePath();
      File directory = new File(absPath.substring(0, absPath.length()-fileName.length()));
      newApi = SimpleAPI.spawnWithLogNoSanitise(fileName, directory);
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
    assert registeredStacks.contains(stack) : "cannot unregister stack, it is not registered";
    registeredStacks.remove(stack);
    reusableStacks.add(stack);
  }

  void removeStack(SymbolTrackingPrincessStack stack) {
    assert registeredStacks.contains(stack) : "cannot remove stack, it is not registered";
    registeredStacks.remove(stack);
    allStacks.remove(stack);
  }

  public List<IExpression> parseStringToTerms(String s) {
    List<IExpression> formula = castToExpression(JavaConversions.seqAsJavaList(api.extractSMTLIBAssertions(new StringReader(s))));

    Set<IExpression> declaredfunctions = PrincessUtil.getVarsAndUIFs(formula);
    for (IExpression var : declaredfunctions) {
      if (var instanceof IConstant) {
        intVariablesCache.put(var.toString(), (ITerm) var);
        for (SymbolTrackingPrincessStack stack : registeredStacks) {
          stack.addSymbol((IConstant)var);
        }
      } else if (var instanceof IAtom) {
        boolVariablesCache.put(((IAtom) var).pred().name(), (IFormula) var);
        for (SymbolTrackingPrincessStack stack : registeredStacks) {
          stack.addSymbol((IAtom)var);
        }
      } else if (var instanceof IFunApp) {
        IFunction fun = ((IFunApp)var).fun();
        functionsCache.put(fun.name(), fun);
        // up to now princess only supports int as return type
        functionsReturnTypes.put(fun, TermType.Integer);
        for (SymbolTrackingPrincessStack stack : registeredStacks) {
          stack.addSymbol(fun);
        }
      }
    }
    return formula;
  }

  private List<IExpression> castToExpression(List<IFormula> formula) {
    List<IExpression> retVal = new ArrayList<>(formula.size());
    for (IFormula f : formula) {
      retVal.add(f);
    }
    return retVal;
  }

  public Appender dumpFormula(IFormula formula) {
    // remove redundant expressions
    final IExpression lettedFormula = PrincessUtil.let(formula, this);
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        out.append("(reset)\n(set-logic AUFLIA)\n");
        Set<IExpression> allVars = getVarsAndUIFs(singleton(lettedFormula));
        Deque<IExpression> declaredFunctions = new ArrayDeque<>(allVars);
        Set<String> doneFunctions = new HashSet<>();
        Set<String> todoAbbrevs = new HashSet<>();

        while (!declaredFunctions.isEmpty()) {
          IExpression var = declaredFunctions.poll();
          String name = getName(var);

          // we don't want to declare variables twice, so doublecheck
          // if we have already found the current variable
          if(doneFunctions.contains(name)) {
            continue;
          }
          doneFunctions.add(name);

          // we do only want to add declare-funs for things we really declared
          // the rest is done afterwards
          if (name.startsWith("abbrev_")) {
            todoAbbrevs.add(name);
            Set<IExpression> varsFromAbbrev = getVarsAndUIFs(singleton(abbrevCache.get(stringToAbbrev.get(name))));
            for (IExpression addVar : Sets.difference(varsFromAbbrev, allVars)) {
              declaredFunctions.push(addVar);
            }
            allVars.addAll(varsFromAbbrev);
          } else {
            out.append("(declare-fun ")
               .append(name);

            // function parameters
            out.append(" (");
            if (var instanceof IFunApp) {
              IFunApp function = (IFunApp) var;
              Iterator<ITerm> args = asJavaIterable(function.args()).iterator();
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
        }

        // now as everything we know from the formula is declared we have to add
        // the abbreviations, too
        for (Entry<IExpression, IExpression> entry : abbrevCache.entrySet()) {
          IExpression abbrev = entry.getKey();
          IExpression fullFormula = entry.getValue();
          String name = getName(getOnlyElement(getVarsAndUIFs(singleton(abbrev))));

          //only add the necessary abbreviations
          if(!todoAbbrevs.contains(name)) {
            continue;
          }

          out.append("(define-fun ")
             .append(name);

          // the type of each abbreviation + the renamed formula
          out.append(" ((abbrev_arg Int)) Int ");
          if (fullFormula instanceof IFormula) {
            out.append("(ite ")
               .append(SMTLineariser.asString(fullFormula))
               .append(" 0 1))\n");
          } else if (fullFormula instanceof ITerm) {
            out.append(SMTLineariser.asString(fullFormula))
               .append(" )\n");
          }

        }

        // now add the final assert
        out.append("(assert ")
           .append(SMTLineariser.asString(lettedFormula))
           .append(")");
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
          for (SymbolTrackingPrincessStack stack : allStacks) {
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
          for (SymbolTrackingPrincessStack stack : allStacks) {
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
  public IFunction declareFun(String name, int nofArgs, TermType returnType) {
    if (functionsCache.containsKey(name)) {
      assert returnType == functionsReturnTypes.get(functionsCache.get(name));
      return functionsCache.get(name);

    } else {
      IFunction funcDecl = api.createFunction(name, nofArgs);
      for (SymbolTrackingPrincessStack stack : allStacks) {
         stack.addSymbol(funcDecl);
      }
      functionsCache.put(name, funcDecl);
      functionsReturnTypes.put(funcDecl, returnType);
      return funcDecl;
    }
  }

  TermType getReturnTypeForFunction(IFunction fun) {
    return functionsReturnTypes.get(fun);
  }

  public IExpression makeFunction(IFunction funcDecl, List<IExpression> args) {
    checkArgument(args.size() == funcDecl.arity(),
        "functiontype has different number of args.");

    final ArrayBuffer<ITerm> argsBuf = new ArrayBuffer<>();
    for (IExpression arg : args) {
      ITerm termArg;
      if (arg instanceof IFormula) { // boolean term -> build ITE(t,0,1), TODO why not ITE(t,1,0) ??
        termArg = new ITermITE((IFormula)arg, new IIntLit(IdealInt.ZERO()), new IIntLit(IdealInt.ONE()));
      } else {
        termArg = (ITerm) arg;
      }
      argsBuf.$plus$eq(termArg);
    }

    IExpression returnFormula = new IFunApp(funcDecl, argsBuf.toSeq());
    TermType returnType = getReturnTypeForFunction(funcDecl);

    // boolean term -> build ITE(t > 0, true, false)
    if (returnType == TermType.Boolean) {
      BooleanFunApplier ap = new BooleanFunApplier(funcDecl);
      return ap.apply(argsBuf);

    } else if (returnType != TermType.Integer) {
      throw new AssertionError("Not possible to have return types for functions other than bool or int.");
    }

    return returnFormula;
  }

  public IExpression abbrev(IExpression expr) {
    IExpression abbrev;
    if (expr instanceof IFormula) {
      abbrev = api.abbrev((IFormula)expr);
    } else if (expr instanceof ITerm) {
      abbrev = api.abbrev((ITerm)expr);
    } else {
      throw new AssertionError("no possibility to create abbreviation for " + expr.getClass());
    }

    for (SymbolTrackingPrincessStack stack : allStacks) {
      stack.addAbbrev(abbrev, expr);
    }

    stringToAbbrev.put(getName(getOnlyElement(getVarsAndUIFs(singleton(abbrev)))), abbrev);
    abbrevCache.put(abbrev, expr);
    return abbrev;
  }

  public Optional<IExpression> fullVersionOfAbbrev(final IExpression expr) {
    return fromNullable(abbrevCache.get(expr));
  }

  public String getVersion() {
    return "Princess (unknown version)";
  }
}
