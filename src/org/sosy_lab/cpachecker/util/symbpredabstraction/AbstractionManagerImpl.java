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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Region;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 * 
 * This class inherits from CtoFormulaConverter to import the stuff there.
 * 
 * @author Philipp Wendler
 */
@Options(prefix="cpas.symbpredabs.mathsat")
public class AbstractionManagerImpl implements AbstractionManager {

  protected final LogManager logger;
  protected final RegionManager rmgr;
  protected final SymbolicFormulaManager smgr;

  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<Region, AbstractionPredicate> absVarToPredicate;
  // and the mapping symbolic variable -> predicate
  private final Map<SymbolicFormula, AbstractionPredicate> symbVarToPredicate;

  @Option
  protected boolean useCache = true;

  private final Map<Region, SymbolicFormula> toConcreteCache;

  public AbstractionManagerImpl(RegionManager pRmgr, SymbolicFormulaManager pSmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, AbstractionManagerImpl.class);
    logger = pLogger;
    rmgr = pRmgr;
    smgr = pSmgr;

    absVarToPredicate = new HashMap<Region, AbstractionPredicate>();
    symbVarToPredicate = new HashMap<SymbolicFormula, AbstractionPredicate>();

    if (useCache) {
      toConcreteCache = new HashMap<Region, SymbolicFormula>();
    } else {
      toConcreteCache = null;
    }
  }

  /**
   * Generates the predicates corresponding to the given atoms.
   */
  protected List<AbstractionPredicate> buildPredicates(Collection<SymbolicFormula> atoms) {
    List<AbstractionPredicate> ret = new ArrayList<AbstractionPredicate>(atoms.size());

    for (SymbolicFormula atom : atoms) {
      ret.add(makePredicate(atom));
    }
    return ret;
  }
  
  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.symbpredabstraction.IAbstractionManager#makePredicate(org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula)
   */
  @Override
  public AbstractionPredicate makePredicate(SymbolicFormula atom) {
    SymbolicFormula var = smgr.createPredicateVariable(atom);
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      Region absVar = rmgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", absVar,
          "from variable", var, "and atom", atom);

      result = new AbstractionPredicate(absVar, var, atom);
      symbVarToPredicate.put(var, result);
      absVarToPredicate.put(absVar, result);
    }
    return result;
  }
  
  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.symbpredabstraction.IAbstractionManager#makeFalsePredicate()
   */
  @Override
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(smgr.makeFalse());
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.symbpredabstraction.IAbstractionManager#getPredicate(org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula)
   */
  @Override
  public AbstractionPredicate getPredicate(SymbolicFormula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      throw new IllegalArgumentException(var + " seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.symbpredabstraction.IAbstractionManager#toConcrete(org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Region)
   */
  @Override
  public SymbolicFormula toConcrete(Region af) {

    Map<Region, SymbolicFormula> cache;
    if (useCache) {
      cache = toConcreteCache;
    } else {
      cache = new HashMap<Region, SymbolicFormula>();
    }
    Deque<Region> toProcess = new ArrayDeque<Region>();

    cache.put(rmgr.makeTrue(), smgr.makeTrue());
    cache.put(rmgr.makeFalse(), smgr.makeFalse());

    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.peek();
      if (cache.containsKey(n)) {
        toProcess.pop();
        continue;
      }
      boolean childrenDone = true;
      SymbolicFormula m1 = null;
      SymbolicFormula m2 = null;

      Triple<Region, Region, Region> parts = rmgr.getIfThenElse(n);
      Region c1 = parts.getSecond();
      Region c2 = parts.getThird();
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
        Region var = parts.getFirst();
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

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.util.symbpredabstraction.IAbstractionManager#makeTrueAbstractionFormula(org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula)
   */
  @Override
  public AbstractionFormula makeTrueAbstractionFormula(SymbolicFormula previousBlockFormula) {
    if (previousBlockFormula == null) {
      previousBlockFormula = smgr.makeTrue();
    }
    return new AbstractionFormula(rmgr.makeTrue(), smgr.makeTrue(), previousBlockFormula);
  }

  @Override
  public RegionManager getRegionManager() {
    return rmgr;
  }
}
