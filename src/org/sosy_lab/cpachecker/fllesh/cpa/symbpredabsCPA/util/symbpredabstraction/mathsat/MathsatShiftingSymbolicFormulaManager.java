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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.ShiftingSymbolicFormulaManager;

public class MathsatShiftingSymbolicFormulaManager extends MathsatSymbolicFormulaManager implements ShiftingSymbolicFormulaManager  {
  
  // We need to distinguish assignments from tests. This is needed to
  // build a formula in SSA form later on, when we have to mapback
  // a counterexample, without adding too many extra variables. Therefore,
  // in the representation of "uninstantiated" symbolic formulas, we
  // use a new binary uninterpreted function ":=" to represent
  // assignments. When we instantiate the formula, we replace this UIF
  // with an equality, because now we have an SSA form
  private final long assignUfDecl;
  
  // cache for replacing assignments
  private final Map<SymbolicFormula, SymbolicFormula> replaceAssignmentsCache = new HashMap<SymbolicFormula, SymbolicFormula>();
  
  public MathsatShiftingSymbolicFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(config, logger);
    
    final int[] msatVarType2 = {super.msatVarType, super.msatVarType};
    assignUfDecl = msat_declare_uif(getMsatEnv(), ":=", MSAT_BOOL, 2, msatVarType2);
  }
  
  @Override
  public SymbolicFormula makeAssignment(SymbolicFormula f1, SymbolicFormula f2) {
    long[] args = {org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f1), org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.getTerm(f2)};

    return encapsulate(msat_make_uif(getMsatEnv(), assignUfDecl, args));
  }

  private boolean isAssignment(long term) {
    return msat_term_get_decl(term) == assignUfDecl;
  }
  
  // ----------------- Complex formula manipulation -----------------

  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    Deque<SymbolicFormula> toProcess = new ArrayDeque<SymbolicFormula>();
    Map<SymbolicFormula, SymbolicFormula> cache = new HashMap<SymbolicFormula, SymbolicFormula>();

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
          long newt = buildMsatVariable(name, idx);
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
            newt = msat_make_equal(getMsatEnv(), newargs[0], newargs[1]);
          
          } else if (msat_term_is_uif(t) != 0) {
            String name = msat_decl_get_name(msat_term_get_decl(t));
            assert name != null;
            
            if (ufCanBeLvalue(name)) {
              int idx = (ssa != null ? ssa.getIndex(name, org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newargs)) : 1);
              if (idx > 0) {
                // ok, the variable has an instance in the SSA, replace it
                newt = buildMsatUF(makeName(name, idx), newargs);
              } else {
                newt = msat_replace_args(getMsatEnv(), t, newargs);
              }
            } else {
              newt = msat_replace_args(getMsatEnv(), t, newargs);
            }
          } else {
            newt = msat_replace_args(getMsatEnv(), t, newargs);
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
        Pair<String, Integer> var = parseName(msat_term_repr(t));
        String name = var.getFirst();
        int idx = var.getSecond();

        if (idx > 0) {
          // ok, the variable is instantiated in the formula
          // retrieve the index in the SSA, and shift
          int ssaidx = ssa.getIndex(name);
          if (ssaidx > 0) {
            idx = ssaidx + idx-1; // calculate new index
            long newt = buildMsatVariable(name, idx);
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
            newt = msat_make_equal(getMsatEnv(), newargs[0], newargs[1]);

          } else {
            if (msat_term_is_uif(t) != 0) {
              String name = msat_decl_get_name(msat_term_get_decl(t));
              assert name != null;
              
              if (ufCanBeLvalue(name)) {
                // we have to shift this uif as well
                Pair<String, Integer> uif = parseName(name);
                name = uif.getFirst();
                int idx = uif.getSecond();
                
                if (idx > 0) {
                  // ok, the UF is instantiated in the formula
                  // retrieve the index in the SSA, and shift
                  SymbolicFormulaList a = org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager.encapsulate(newargs);

                  int ssaidx = ssa.getIndex(name, a);
                  if (ssaidx > 0) {
                    idx = ssaidx + idx-1; // calculate new index
                    newt = buildMsatUF(makeName(name, idx), newargs);
                  } else {
                    newt = msat_replace_args(getMsatEnv(), t, newargs);
                  }

                  if (lSSAMapBuilder.getIndex(name, a) < idx) {
                    lSSAMapBuilder.setIndex(name, a, idx);
                  }
                  
                } else {
                  // the UF is not instantiated, keep it as is
                  newt = msat_replace_args(getMsatEnv(), t, newargs);
                }
              } else {
                newt = msat_replace_args(getMsatEnv(), t, newargs);
              }
            } else { // "normal" non-variable term
              newt = msat_replace_args(getMsatEnv(), t, newargs);
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
            newt = msat_make_equal(getMsatEnv(), newargs[0], newargs[1]);
          } else {
            newt = msat_replace_args(getMsatEnv(), t, newargs);
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
        Pair<String, Integer> var = parseName(msat_term_repr(t));
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
            if (ufCanBeLvalue(name)) {
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
