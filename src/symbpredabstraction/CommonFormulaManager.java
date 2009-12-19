/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;

import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.FormulaManager;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import cmdline.CPAMain;

import common.Pair;
import common.Triple;

/**
 * Abstract super class for classes implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries. 
 * @author Philipp Wendler
 */
public abstract class CommonFormulaManager implements FormulaManager {

  protected final AbstractFormulaManager amgr;
  protected final SymbolicFormulaManager smgr;
  
  // Here we keep the mapping abstract predicate ->
  // (symbolic formula representing the variable, symbolic formula representing the atom)
  private final Map<Predicate, Pair<SymbolicFormula, SymbolicFormula>> predicateToMsatAtom;
  // and the reverse mapping symbolic variable -> predicate
  private final Map<SymbolicFormula, Predicate> msatVarToPredicate;

  private final boolean useCache;
  private final Map<AbstractFormula, SymbolicFormula> toConcreteCache;

  public CommonFormulaManager(AbstractFormulaManager pAmgr, SymbolicFormulaManager pSmgr) {
    amgr = pAmgr;
    smgr = pSmgr;
    
    predicateToMsatAtom = new HashMap<Predicate, Pair<SymbolicFormula, SymbolicFormula>>();
    msatVarToPredicate = new HashMap<SymbolicFormula, Predicate>();
    
    useCache = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.useCache");
    if (useCache) {
        toConcreteCache = new HashMap<AbstractFormula, SymbolicFormula>();
    } else {
      toConcreteCache = null;
    }
  }
  
  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  protected Predicate makePredicate(SymbolicFormula var, SymbolicFormula atom) {
    if (msatVarToPredicate.containsKey(var)) {
      return msatVarToPredicate.get(var);
    } else {
      Predicate result = amgr.createPredicate();

      CPAMain.logManager.log(Level.FINEST, "Created predicate", result,
                     "from variable", var, "and atom", atom);

      predicateToMsatAtom.put(result, new Pair<SymbolicFormula, SymbolicFormula>(var, atom));
      msatVarToPredicate.put(var, result);
      return result;
    }
  }
  
  protected Pair<? extends SymbolicFormula, ? extends SymbolicFormula> getPredicateVarAndAtom(Predicate p) {
    return predicateToMsatAtom.get(p);
  }
  
  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier. 
   * @return a Predicate
   */
  protected Predicate getPredicate(SymbolicFormula var) {
    Predicate result = msatVarToPredicate.get(var);
    if (var == null) {
      throw new IllegalArgumentException(var + " seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }
  
  /**
   * Given an abstract formula (which is a BDD over the predicates), build
   * its concrete representation (which is a MathSAT formula corresponding
   * to the BDD, in which each predicate is replaced with its definition)
   */
  @Override
  public SymbolicFormula toConcrete(AbstractFormula af) {

      Map<AbstractFormula, SymbolicFormula> cache;
      if (useCache) {
          cache = toConcreteCache;
      } else {
          cache = new HashMap<AbstractFormula, SymbolicFormula>();
      }
      Stack<AbstractFormula> toProcess = new Stack<AbstractFormula>();

      cache.put(amgr.makeTrue(), smgr.makeTrue());
      cache.put(amgr.makeFalse(), smgr.makeFalse());

      toProcess.push(af);
      while (!toProcess.empty()) {
          AbstractFormula n = toProcess.peek();
          if (cache.containsKey(n)) {
              toProcess.pop();
              continue;
          }
          boolean childrenDone = true;
          SymbolicFormula m1 = null;
          SymbolicFormula m2 = null;
          
          Triple<Predicate, AbstractFormula, AbstractFormula> parts = amgr.getIfThenElse(n);
          AbstractFormula c1 = parts.getSecond();
          AbstractFormula c2 = parts.getThird();
          if (!cache.containsKey(c1)) {
              toProcess.push(c1);
              childrenDone = false;
          } else {
              m1 = cache.get(c1);
          }
          if (!cache.containsKey(c2)) {
              toProcess.push(c2);
              childrenDone = false;
          } else {
              m2 = cache.get(c2);
          }
          if (childrenDone) {
              assert m1 != null;
              assert m2 != null;

              toProcess.pop();
              Predicate var = parts.getFirst();
              assert(predicateToMsatAtom.containsKey(var));

              SymbolicFormula atom = predicateToMsatAtom.get(var).getSecond();
              
              SymbolicFormula ite = smgr.makeIfThenElse(atom, m1, m2);
              cache.put(n, ite);
          }
      }

      assert(cache.containsKey(af));

      return cache.get(af);
  }

}