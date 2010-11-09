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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.assumptions.Assumption;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import com.google.common.base.Joiner;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 * 
 * This class inherits from CtoFormulaConverter to import the stuff there.
 * 
 * @author Philipp Wendler
 */
@Options(prefix="cpas.symbpredabs.mathsat")
public class CommonFormulaManager extends CtoFormulaConverter implements FormulaManager {

  protected final AbstractFormulaManager amgr;

  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<AbstractFormula, Predicate> absVarToPredicate;
  // and the mapping symbolic variable -> predicate
  private final Map<SymbolicFormula, Predicate> symbVarToPredicate;

  @Option
  protected boolean useCache = true;

  private final Map<AbstractFormula, SymbolicFormula> toConcreteCache;

  public CommonFormulaManager(AbstractFormulaManager pAmgr, SymbolicFormulaManager pSmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pSmgr, pLogger);
    config.inject(this, CommonFormulaManager.class);
    amgr = pAmgr;

    absVarToPredicate = new HashMap<AbstractFormula, Predicate>();
    symbVarToPredicate = new HashMap<SymbolicFormula, Predicate>();

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
  @Override
  public Predicate makePredicate(SymbolicFormula var, SymbolicFormula atom) {
    Predicate result = symbVarToPredicate.get(var);
    if (result == null) {
      AbstractFormula absVar = amgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", absVar,
          "from variable", var, "and atom", atom);

      result = new Predicate(absVar, var, atom);
      symbVarToPredicate.put(var, result);
      absVarToPredicate.put(absVar, result);
    }
    return result;
  }
  
  @Override
  public Predicate makeTruePredicate() {
    return makePredicate(smgr.createPredicateVariable(smgr.makeTrue()), smgr.makeTrue());
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
    Deque<AbstractFormula> toProcess = new ArrayDeque<AbstractFormula>();

    cache.put(amgr.makeTrue(), smgr.makeTrue());
    cache.put(amgr.makeFalse(), smgr.makeFalse());

    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      AbstractFormula n = toProcess.peek();
      if (cache.containsKey(n)) {
        toProcess.pop();
        continue;
      }
      boolean childrenDone = true;
      SymbolicFormula m1 = null;
      SymbolicFormula m2 = null;

      Triple<AbstractFormula, AbstractFormula, AbstractFormula> parts = amgr.getIfThenElse(n);
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
        AbstractFormula var = parts.getFirst();
        assert(absVarToPredicate.containsKey(var));

        SymbolicFormula atom = absVarToPredicate.get(var).getSymbolicAtom();

        SymbolicFormula ite = smgr.makeIfThenElse(atom, m1, m2);
        cache.put(n, ite);
      }
    }

    SymbolicFormula result = cache.get(af);
    assert result != null;

    return result;
  }

  @Override
  public Abstraction makeTrueAbstraction(SymbolicFormula previousBlockFormula) {
    if (previousBlockFormula == null) {
      previousBlockFormula = smgr.makeTrue();
    }
    return new Abstraction(amgr.makeTrue(), smgr.makeTrue(), previousBlockFormula);
  }

  // the rest of this class is related only to symbolic formulas

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(smgr.makeTrue(), SSAMap.emptySSAMap(), 0, smgr.makeTrue(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(smgr.makeTrue(), oldFormula.getSsa(), 0,
        oldFormula.getReachingPathsFormula(), oldFormula.getBranchingCounter());
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

    Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    SymbolicFormula newFormula2 = smgr.makeAnd(formula2, pm.getFirst().getFirst());
    SymbolicFormula newFormula1 = smgr.makeAnd(formula1, pm.getFirst().getSecond());

    SymbolicFormula newFormula = smgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());
    SymbolicFormula newReachingPathsFormula
    = smgr.makeOr(pF1.getReachingPathsFormula(), pF2.getReachingPathsFormula());
    int newBranchingCounter = Math.max(pF1.getBranchingCounter(), pF2.getBranchingCounter());

    return new PathFormula(newFormula, newSsa, newLength,
        newReachingPathsFormula, newBranchingCounter);
  }

  /**
   * Used for computing an abstraction using an assumption formula
   * formula of the assumption is updated accordingly based on the {@link SSAMap} of
   * the path formula for the edge.
   * Then the conjunction of these two formulas is computed and a new path formula
   * constructed using the this conjunction and a merged ssa map is returned.
   * @param pPathFormula a PathFormula
   * @param pAssumptionFormula a PathFormula
   * @return an updated path formula which appends assumption to the path formula
   */
  public PathFormula makeAnd(PathFormula pPathFormula, PathFormula pAssumptionFormula) {

    SSAMap ssaMapOfPf = pPathFormula.getSsa();
//    SSAMap ssaMapOfAsmpt = pAssumptionFormula.getSsa();
    
    SymbolicFormula formulaOfPf = pPathFormula.getSymbolicFormula();
    SymbolicFormula formulaOfAsmpt = pAssumptionFormula.getSymbolicFormula();
    
    //SSAMap mergedSSAMap = SSAMap.merge(ssaMapOfPf, ssaMapOfAsmpt);
    // update the assumption formula with the new ssa index
    //SymbolicFormula updatedAsmptFormula = smgr.instantiate(smgr.uninstantiate(formulaOfAsmpt), mergedSSAMap);
    
    SymbolicFormula retFormula = smgr.makeAnd(formulaOfPf, formulaOfAsmpt);
    return new PathFormula(retFormula, ssaMapOfPf, pPathFormula.getLength(), 
        pPathFormula.getReachingPathsFormula(), pPathFormula.getBranchingCounter());
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param ssa2 an SSAMap
   * @return A pair (SymbolicFormula, SSAMap)
   */
  private Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    SymbolicFormula mt1 = smgr.makeTrue();
    SymbolicFormula mt2 = smgr.makeTrue();

    for (String var : result.allVariables()) {
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        SymbolicFormula t = makeSSAMerger(var, Math.max(i2, 1), i1);
        mt2 = smgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        SymbolicFormula t = makeSSAMerger(var, Math.max(i1, 1), i2); 
        mt1 = smgr.makeAnd(mt1, t); 
      }
    }

    for (Pair<String, SymbolicFormulaList> f : result.allFunctions()) {
      String name = f.getFirst();
      SymbolicFormulaList args = f.getSecond();
      int i1 = ssa1.getIndex(f);
      int i2 = ssa2.getIndex(f);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        SymbolicFormula t = makeSSAMerger(name, args, Math.max(i2, 1), i1);
        mt2 = smgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        SymbolicFormula t = makeSSAMerger(name, args, Math.max(i1, 1), i2); 
        mt1 = smgr.makeAnd(mt1, t); 
      }
    }

    Pair<SymbolicFormula, SymbolicFormula> sp =
      new Pair<SymbolicFormula, SymbolicFormula>(mt1, mt2);
    return new Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap>(sp, result);
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private SymbolicFormula makeSSAMerger(String var, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    SymbolicFormula initialVar = smgr.makeVariable(var, iSmaller);
    SymbolicFormula result = smgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      SymbolicFormula currentVar = smgr.makeVariable(var, i);
      SymbolicFormula e = smgr.makeEqual(currentVar, initialVar);
      result = smgr.makeAnd(result, e);
    }
    return result;
  }

  private SymbolicFormula makeSSAMerger(String name,
      SymbolicFormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    SymbolicFormula intialFunc = smgr.makeUIF(name, args, iSmaller);
    SymbolicFormula result = smgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      SymbolicFormula currentFunc = smgr.makeUIF(name, args, i);
      SymbolicFormula e = smgr.makeEqual(currentFunc, intialFunc);
      result = smgr.makeAnd(result, e);
    }
    return result;
  }

  protected void dumpFormulaToFile(SymbolicFormula f, File outputFile) {
    try {
      Files.writeFile(outputFile, smgr.dumpFormula(f));
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to save formula to file ", outputFile.getPath(), "(", e.getMessage(), ")");
    }
  }

  private static final Joiner LINE_JOINER = Joiner.on('\n');

  protected void printFormulasToFile(Iterable<SymbolicFormula> f, File outputFile) {
    try {
      Files.writeFile(outputFile, LINE_JOINER.join(f));
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to save formula to file ", outputFile.getPath(), "(", e.getMessage(), ")");
    }
  }

  public PathFormula makePathFormulaAndAssumption(PathFormula pPathFormula, Assumption pAssumption) {
    SSAMap ssa = pPathFormula.getSsa();
    SymbolicFormula assumptionFormula =  smgr.instantiate(pAssumption.getDischargeableFormula(), ssa);
    SymbolicFormula resultFormula = smgr.makeAnd(pPathFormula.getSymbolicFormula(), assumptionFormula);
    return new PathFormula(resultFormula, ssa, pPathFormula.getLength(),
        pPathFormula.getReachingPathsFormula(), pPathFormula.getBranchingCounter());
  }
}
