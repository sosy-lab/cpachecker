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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;


/**
 * Abstract super class for classes implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 * @author Philipp Wendler
 */
@Options(prefix="cpas.symbpredabs.mathsat")
public abstract class CommonFormulaManager implements FormulaManager {

  protected final AbstractFormulaManager amgr;
  protected final SymbolicFormulaManager smgr;
  protected final LogManager logger;

  // Here we keep the mapping abstract predicate ->
  // (symbolic formula representing the variable, symbolic formula representing the atom)
  private final Map<Predicate, Pair<SymbolicFormula, SymbolicFormula>> predicateToVarAndAtom;
  // and the reverse mapping symbolic variable -> predicate
  private final Map<SymbolicFormula, Predicate> symbVarToPredicate;

  @Option
  protected boolean useCache = true;

  private final Map<AbstractFormula, SymbolicFormula> toConcreteCache;

  public CommonFormulaManager(AbstractFormulaManager pAmgr, SymbolicFormulaManager pSmgr,
                    Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, CommonFormulaManager.class);
    amgr = pAmgr;
    smgr = pSmgr;
    logger = pLogger;

    predicateToVarAndAtom = new HashMap<Predicate, Pair<SymbolicFormula, SymbolicFormula>>();
    symbVarToPredicate = new HashMap<SymbolicFormula, Predicate>();

    if (useCache) {
      toConcreteCache = new HashMap<AbstractFormula, SymbolicFormula>();
    } else {
      toConcreteCache = null;
    }
  }

  /**
   * Generates the predicates corresponding to the given atoms.
   */
  protected Set<Predicate> buildPredicates(Collection<SymbolicFormula> atoms) {
    Set<Predicate> ret = new HashSet<Predicate>(atoms.size());

    for (SymbolicFormula atom : atoms) {
      ret.add(makePredicate(smgr.createPredicateVariable(atom), atom));
    }
    return ret;
  }

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  @Override
  public Predicate makePredicate(SymbolicFormula var, SymbolicFormula atom) {
    if (symbVarToPredicate.containsKey(var)) {
      return symbVarToPredicate.get(var);
    } else {
      Predicate result = amgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", result,
                     "from variable", var, "and atom", atom);

      predicateToVarAndAtom.put(result, new Pair<SymbolicFormula, SymbolicFormula>(var, atom));
      symbVarToPredicate.put(var, result);
      return result;
    }
  }

  /**
   * Get the symbolic formulas for the variable and the atom which belong to a
   * predicate.
   * @param p A predicate which has been return by {@link #makePredicate(SymbolicFormula, SymbolicFormula)}
   * @return The values passed to the makePredicate call (symbolic formula for var and atom)
   */
  @Override
  public Pair<? extends SymbolicFormula, ? extends SymbolicFormula> getPredicateVarAndAtom(Predicate p) {
    return predicateToVarAndAtom.get(p);
  }

  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier.
   * @return a Predicate
   */
  @Override
  public Predicate getPredicate(SymbolicFormula var) {
    Predicate result = symbVarToPredicate.get(var);
    if (result == null) {
      throw new IllegalArgumentException(var + "seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }
  
  /**
   * Create the formula \bigwedge (predvar <-> preddef)
   * All variables will be instantiated with the indices from the given SSAMap.
   * @param predicates The predicates to include in the formula.
   * @param ssa The SSAMap for the instantiation of the formula.
   * @return The above formula.
   */
  protected SymbolicFormula buildPredicateFormula(Collection<Predicate> predicates,
                                                  SSAMap ssa) {
    ssa = new SSAMap(ssa); // clone ssa map because we need to change it
    
    Set<String> allvars = new HashSet<String>();
    Set<Pair<String, SymbolicFormula[]>> allfuncs = new HashSet<Pair<String, SymbolicFormula[]>>();
    SymbolicFormula preddef = smgr.makeTrue();

    for (Predicate p : predicates) {
        SymbolicFormula var = getPredicateVarAndAtom(p).getFirst();
        SymbolicFormula def = getPredicateVarAndAtom(p).getSecond();
        smgr.collectVarNames(def, allvars, allfuncs);
        
        // build the formula (var <-> def)
        SymbolicFormula equiv = smgr.makeEquivalence(var, def);

        // and add it to the list of definitions
        preddef = smgr.makeAnd(preddef, equiv);
    }
    
    // update the SSA map, by instantiating all the uninstantiated
    // variables that occur in the predicates definitions (at index 1)
    for (String var : allvars) {
      if (ssa.getIndex(var) < 0) {
        ssa.setIndex(var, 1);
      }
    }
    Map<SymbolicFormula, SymbolicFormula> cache =
      new HashMap<SymbolicFormula, SymbolicFormula>();
    for (Pair<String, SymbolicFormula[]> p : allfuncs) {
      SymbolicFormula[] args =
        smgr.getInstantiatedAt(p.getSecond(), ssa, cache);
      if (ssa.getIndex(p.getFirst(), args) < 0) {
        ssa.setIndex(p.getFirst(), args, 1);
      }
    }

    // instantiate the definitions with the right SSA
    return smgr.instantiate(preddef, ssa);
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
              assert(predicateToVarAndAtom.containsKey(var));

              SymbolicFormula atom = predicateToVarAndAtom.get(var).getSecond();

              SymbolicFormula ite = smgr.makeIfThenElse(atom, m1, m2);
              cache.put(n, ite);
          }
      }

      assert(cache.containsKey(af));

      return cache.get(af);
  }

  /**
   * Creates a new path formula representing an OR of the two arguments. Differently
   * from {@link SymbolicFormulaManager#makeOr(SymbolicFormula, SymbolicFormula)},
   * it also merges the SSA maps and creates the necessary adjustments to the
   * formulas if the two SSA maps contain different values for the same variables.
   *
   * @param pF1 a PathFormula
   * @param pF2 a PathFormula
   * @return (pF1 | pF2)
   */
  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    SymbolicFormula formula1 = pF1.getSymbolicFormula();
    SymbolicFormula formula2 = pF2.getSymbolicFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = smgr.mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    SymbolicFormula newFormula2 = smgr.makeAnd(formula2, pm.getFirst().getFirst());
    SymbolicFormula newFormula1 = smgr.makeAnd(formula1, pm.getFirst().getSecond());

    SymbolicFormula newFormula = smgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    return new PathFormula(newFormula, newSsa);
  }

  @Override
  public void dumpFormulasToFile(Iterable<SymbolicFormula> f, File outputFile) {
    Iterator<SymbolicFormula> it = f.iterator();
    SymbolicFormula t = it.next();
    
    while (it.hasNext()) { 
      t = smgr.makeAnd(t, it.next());
    }
    
    try {
      Files.writeFile(outputFile, smgr.dumpFormula(t), false);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to save formula to file ", outputFile.getPath(), "(", e.getMessage(), ")");
    }
  }
}