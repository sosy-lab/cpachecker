/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat;

import static mathsat.api.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

@Options(prefix="cpas.symbpredabs.mathsat")
public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager  {
  
  final org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager mInternalSFM;
  
  @Option
  private boolean useIntegers = false;
  
  // We need to distinguish assignments from tests. This is needed to
  // build a formula in SSA form later on, when we have to mapback
  // a counterexample, without adding too many extra variables. Therefore,
  // in the representation of "uninstantiated" symbolic formulas, we
  // use a new binary uninterpreted function ":=" to represent
  // assignments. When we instantiate the formula, we replace this UIF
  // with an equality, because now we have an SSA form
  private final long assignUfDecl;
  
  // datatype to use for variables, when converting them to mathsat vars
  // can be either MSAT_REAL or MSAT_INT
  // Note that MSAT_INT does not mean that we support the full linear
  // integer arithmetic (LIA)! At the moment, interpolation doesn't work on
  // LIA, only difference logic or on LRA (i.e. on the rationals). However
  // by setting the vars to be MSAT_INT, the solver tries some heuristics
  // that might work (e.g. tightening of a < b into a <= b - 1, splitting
  // negated equalities, ...)
  private final int msatVarType;
  
  // cache for replacing assignments
  private final Map<SymbolicFormula, SymbolicFormula> replaceAssignmentsCache = new HashMap<SymbolicFormula, SymbolicFormula>();
  
  public MathsatSymbolicFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, MathsatSymbolicFormulaManager.class);
    mInternalSFM = new org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager(config, logger);
    
    msatVarType = useIntegers ? MSAT_INT : MSAT_REAL;
    final int[] msatVarType2 = {msatVarType, msatVarType};
    assignUfDecl = msat_declare_uif(mInternalSFM.getMsatEnv(), ":=", MSAT_BOOL, 2, msatVarType2);
  }

  public org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager getInternalSFM() {
    return mInternalSFM;
  }
  
  long getMsatEnv() {
    return mInternalSFM.getMsatEnv();
  }

  long createEnvironment(boolean shared, boolean ghostFilter) {
    return mInternalSFM.createEnvironment(shared, ghostFilter);
  }

  // ----------------- Boolean formulas -----------------
  
  @Override
  public boolean isBoolean(SymbolicFormula f) {
    return mInternalSFM.isBoolean(f);
  }
  
  @Override
  public SymbolicFormula makeTrue() {
    return mInternalSFM.makeTrue();
  }
  
  @Override
  public SymbolicFormula makeFalse() {
    return mInternalSFM.makeFalse();
  }

  @Override
  public SymbolicFormula makeNot(SymbolicFormula f) {
    return mInternalSFM.makeNot(f);
  }

  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeAnd(f1, f2);
  }

  @Override
  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeOr(f1, f2);
  }

  @Override
  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeEquivalence(f1, f2);
  }

  @Override
  public SymbolicFormula makeIfThenElse(SymbolicFormula condition, SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeIfThenElse(condition, f1, f2);
  }
  
  
  // ----------------- Numeric formulas -----------------

  @Override
  public SymbolicFormula makeNegate(SymbolicFormula f) {
    return mInternalSFM.makeNegate(f);
  }

  @Override
  public SymbolicFormula makeNumber(int i) {
    return mInternalSFM.makeNumber(i);
  }
  
  @Override
  public SymbolicFormula makeNumber(String i) {
    return mInternalSFM.makeNumber(i);
  }
  
  @Override
  public SymbolicFormula makePlus(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makePlus(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeMinus(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeMinus(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeDivide(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeDivide(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeModulo(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeModulo(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeMultiply(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeMultiply(f1, f2);
  }
  
  // ----------------- Numeric relations -----------------
  
  @Override
  public SymbolicFormula makeEqual(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeEqual(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeGt(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeGt(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeGeq(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeGeq(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeLt(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeLt(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeLeq(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeLeq(f1, f2);
  }
  
  // ----------------- Bit-manipulation functions -----------------

  @Override
  public SymbolicFormula makeBitwiseNot(SymbolicFormula f) {
    return mInternalSFM.makeBitwiseNot(f);
  }

  @Override
  public SymbolicFormula makeBitwiseAnd(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeBitwiseAnd(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeBitwiseOr(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeBitwiseOr(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeBitwiseXor(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeBitwiseXor(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeShiftLeft(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeShiftLeft(f1, f2);
  }
  
  @Override
  public SymbolicFormula makeShiftRight(SymbolicFormula f1, SymbolicFormula f2) {
    return mInternalSFM.makeShiftRight(f1, f2);
  }

  // ----------------- Uninterpreted functions -----------------
  
  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormulaList args) {
    return mInternalSFM.makeUIF(name, args);
  }

  @Override
  public SymbolicFormula makeUIF(String name, SymbolicFormulaList args, int idx) {
    return mInternalSFM.makeUIF(name, args, idx);
  }

  // ----------------- Other formulas -----------------

  @Override
  public SymbolicFormula makeString(int i) {
    return mInternalSFM.makeString(i);
  }

  @Override
  public SymbolicFormula makeVariable(String var, int idx) {
    return mInternalSFM.makeVariable(var, idx);
  }
  
  @Override
  public SymbolicFormula makeAssignment(SymbolicFormula f1, SymbolicFormula f2) {
    long[] args = {org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f1), org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f2)};

    return org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_make_uif(mInternalSFM.getMsatEnv(), assignUfDecl, args));
  }

  private boolean isAssignment(long term) {
    return msat_term_get_decl(term) == assignUfDecl;
  }

  
  // ----------------- Convert to list -----------------
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula pF) {
    return mInternalSFM.makeList(pF);
  }
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula pF1, SymbolicFormula pF2) {
    return mInternalSFM.makeList(pF1, pF2);
  }
  
  @Override
  public SymbolicFormulaList makeList(SymbolicFormula... pF) {
    return mInternalSFM.makeList(pF);
  }
  
  // ----------------- Complex formula manipulation -----------------
  
  @Override
  public SymbolicFormula createPredicateVariable(SymbolicFormula atom) {
    return mInternalSFM.createPredicateVariable(atom);
  }

  @Override
  public String dumpFormula(SymbolicFormula f) {
    return mInternalSFM.dumpFormula(f);
  }

  @Override
  public void dumpAbstraction(SymbolicFormula curState, SymbolicFormula edgeFormula,
      SymbolicFormula predDef, List<SymbolicFormula> importantPreds) {
  }

  @Override
  public SymbolicFormula parseInfix(String s) {
    return mInternalSFM.parseInfix(s);
  }

  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    return instantiate(f, ssa, new HashMap<SymbolicFormula, SymbolicFormula>());
  }

  @Override
  public SymbolicFormulaList instantiate(SymbolicFormulaList f, SSAMap ssa) {
    long[] args = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f);
    long[] result = new long[args.length];
    Map<SymbolicFormula, SymbolicFormula> cache = new HashMap<SymbolicFormula, SymbolicFormula>();
    
    for (int i = 0; i < args.length; i++) {
      result[i] = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(instantiate(org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(args[i]), ssa, cache));
    }
    return org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(result);
  }

  private SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa,
                                      Map<SymbolicFormula, SymbolicFormula> cache) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = msat_term_repr(t);
        int idx = (ssa != null ? ssa.getIndex(name) : 1);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = mInternalSFM.buildMsatVariable(name, idx);
          cache.put(tt, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newt));
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(tt, tt);
        }

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          SymbolicFormula c = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_term_get_arg(t, i));
          SymbolicFormula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (isAssignment(t)) {
            // now we replace our "fake" assignment with an equality
            assert newargs.length == 2;
            newt = msat_make_equal(mInternalSFM.getMsatEnv(), newargs[0], newargs[1]);
          
          } else if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;
            
            if (mInternalSFM.ufCanBeLvalue(name)) {
              int idx = (ssa != null ? ssa.getIndex(name, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newargs)) : 1);
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = mInternalSFM.buildMsatUF(mInternalSFM.makeName(name, idx), newargs);
              } else {
                newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
              }
            } else {
              newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
            }
          } else {
            newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
          }
          
          cache.put(tt, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
  }

  @Override
  public SymbolicFormula uninstantiate(SymbolicFormula f) {
    return mInternalSFM.uninstantiate(f);
  }
  
  /**
   * As a side effect, this method does the same thing as {@link #replaceAssignments(SymbolicFormula)}
   * to the formula.
   */
  @Override
  public Pair<SymbolicFormula, SSAMap> shift(SymbolicFormula f, SSAMap ssa) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Map<SymbolicFormula, SymbolicFormula> cache = new HashMap<SymbolicFormula, SymbolicFormula>();

    SSAMapBuilder lSSAMapBuilder = SSAMap.emptySSAMap().builder();
    
    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        toProcess.pop();
        // check whether this is an instantiated variable
        Pair<String, Integer> var = mInternalSFM.parseName(msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();

        if (idx > 0) {
          // ok, the variable is instantiated in the formula
          // retrieve the index in the SSA, and shift
          int ssaidx = ssa.getIndex(name);
          if (ssaidx > 0) {
            idx = ssaidx + idx-1; // calculate new index
            long newt = mInternalSFM.buildMsatVariable(name, idx);
            cache.put(tt, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newt));
          } else {
            cache.put(tt, tt);
          }
          if (lSSAMapBuilder.getIndex(name) < idx) {
            lSSAMapBuilder.setIndex(name, idx);
          }
        } else {
          // the variable is not instantiated, keep it as is
          cache.put(tt, tt);
        }

      } else {
        boolean childrenDone = true;
        long[] newargs = new long[msat_term_arity(t)];
        for (int i = 0; i < newargs.length; ++i) {
          SymbolicFormula c = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_term_get_arg(t, i));
          SymbolicFormula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }

        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (isAssignment(t)) {
            newt = msat_make_equal(mInternalSFM.getMsatEnv(), newargs[0], newargs[1]);

          } else {
            if (msat_term_is_uif(t) != 0) {
              String name = msat_decl_get_name(msat_term_get_decl(t));
              assert name != null;
              
              if (mInternalSFM.ufCanBeLvalue(name)) {
                // we have to shift this uif as well
                Pair<String, Integer> uif = mInternalSFM.parseName(name);
                name = uif.getFirst();
                int idx = uif.getSecond();
                
                if (idx > 0) {
                  // ok, the UF is instantiated in the formula
                  // retrieve the index in the SSA, and shift
                  SymbolicFormulaList a = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newargs);

                  int ssaidx = ssa.getIndex(name, a);
                  if (ssaidx > 0) {
                    idx = ssaidx + idx-1; // calculate new index
                    newt = mInternalSFM.buildMsatUF(mInternalSFM.makeName(name, idx), newargs);
                  } else {
                    newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
                  }

                  if (lSSAMapBuilder.getIndex(name, a) < idx) {
                    lSSAMapBuilder.setIndex(name, a, idx);
                  }
                  
                } else {
                  // the UF is not instantiated, keep it as is
                  newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
                }
              } else {
                newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
              }
            } else { // "normal" non-variable term
              newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
            }
          }
          
          cache.put(tt, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newt));
        }
      }
    }

    SymbolicFormula result = cache.get(f);
    assert result != null;
    return new Pair<SymbolicFormula, SSAMap>(result, lSSAMapBuilder.build());
  }
  
  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula.
  @Override
  public SymbolicFormula getBitwiseAxioms(SymbolicFormula f) {
    return mInternalSFM.getBitwiseAxioms(f);
  }

  @Override
  public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
      boolean splitArithEqualities, boolean conjunctionsOnly) {
    return mInternalSFM.extractAtoms(f, splitArithEqualities, conjunctionsOnly);
  }

  /**
   * The path formulas created by this class have an uninterpreted function :=
   * where an assignment should be. This method replaces all those appearances
   * by equalities (which is a valid representation of an assignment for a SSA
   * formula).
   */
  @Override
  public SymbolicFormula replaceAssignments(SymbolicFormula f) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Map<SymbolicFormula, SymbolicFormula> cache = replaceAssignmentsCache;

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.peek();
      if (cache.containsKey(tt)) {
        toProcess.pop();
        continue;
      }
      final long t = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(tt);
      
      if (msat_term_arity(t) == 0) {
        cache.put(tt, tt);
      
      } else {
        long[] newargs = new long[msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < newargs.length; ++i) {
          SymbolicFormula c = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_term_get_arg(t, i));
          SymbolicFormula newC = cache.get(c);
          if (newC != null) {
            newargs[i] = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(newC);
          } else {
            toProcess.push(c);
            childrenDone = false;
          }
        }
        
        if (childrenDone) {
          toProcess.pop();
          long newt;
          if (isAssignment(t)) {
            newt = msat_make_equal(mInternalSFM.getMsatEnv(), newargs[0], newargs[1]);
          } else {
            newt = msat_replace_args(mInternalSFM.getMsatEnv(), t, newargs);
          }
          cache.put(tt, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newt));
        }
      }
    }
    
    SymbolicFormula result = cache.get(f);
    assert result != null;
    return result;
  }

  /**
   * returns an SSA map for the instantiated formula f
   */
  @Override
  public SSAMap extractSSA(SymbolicFormula f) {
    SSAMapBuilder lSSAMapBuilder = SSAMap.emptySSAMap().builder();
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Set<SymbolicFormula> cache = new HashSet<SymbolicFormula>();

    toProcess.push(f);
    while (!toProcess.isEmpty()) {
      final SymbolicFormula tt = toProcess.pop();
      if (cache.contains(tt)) {
        continue;
      }
      cache.add(tt);
      final long t = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(tt);

      if (msat_term_is_variable(t) != 0) {
        Pair<String, Integer> var = mInternalSFM.parseName(msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();
        if (idx > lSSAMapBuilder.getIndex(name)) {
          lSSAMapBuilder.setIndex(name, idx);
        }
      } else {
        int arity = msat_term_arity(t);
        for (int i = 0; i < arity; ++i) {
          toProcess.push(org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_term_get_arg(t, i)));
        }
      }
    }

    return lSSAMapBuilder.build();
  }

  @Override
  public void collectVarNames(SymbolicFormula f, Set<String> vars,
                              Set<Pair<String, SymbolicFormulaList>> lvals) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();

    toProcess.push(f);
    // TODO - this assumes the term is small! There is no memoizing yet!!
    while (!toProcess.isEmpty()) {
        final long t = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(toProcess.pop());

        if (msat_term_is_variable(t) != 0) {
          vars.add(msat_term_repr(t));
        
        } else {
          final int arity = msat_term_arity(t);
          for (int i = 0; i < arity; ++i) {
            toProcess.push(org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(msat_term_get_arg(t, i)));
          }
          
          if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            if (mInternalSFM.ufCanBeLvalue(name)) {
              long[] a = new long[arity];
              for (int i = 0; i < arity; ++i) {
                a[i] = msat_term_get_arg(t, i);
              }
              SymbolicFormulaList aa = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(a);
              lvals.add(new Pair<String, SymbolicFormulaList>(name, aa));
            }
          }
        }
    }
  }
}
