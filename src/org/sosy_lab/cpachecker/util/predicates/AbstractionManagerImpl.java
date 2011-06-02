/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.base.Joiner;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 *
 * @author Philipp Wendler
 */
@Options(prefix="cpa.predicate")
public class AbstractionManagerImpl implements AbstractionManager {

  public static interface AbstractionPredicatesMXBean {
    int getNumberOfPredicates();
    String getPredicates();
  }

  private class AbstractionPredicatesMBean extends AbstractMBean implements AbstractionPredicatesMXBean {
    public AbstractionPredicatesMBean() {
      super("org.sosy_lab.cpachecker:type=predicate,name=AbstractionPredicates", logger);
      register();
    }
    @Override
    public int getNumberOfPredicates() {
      return numberOfPredicates;
    }
    @Override
    public String getPredicates() {
      // TODO this may run into a ConcurrentModificationException
      return Joiner.on('\n').join(absVarToPredicate.values());
    }
  }

  private volatile int numberOfPredicates = 0;

  private final LogManager logger;
  private final RegionManager rmgr;
  private final FormulaManager fmgr;

  // Here we keep the mapping abstract predicate variable -> predicate
  private final Map<Region, AbstractionPredicate> absVarToPredicate;
  // and the mapping symbolic variable -> predicate
  private final Map<Formula, AbstractionPredicate> symbVarToPredicate;

  @Option(description="use caching of region to formula conversions")
  private boolean useCache = true;

  private final Map<Region, Formula> toConcreteCache;

  public AbstractionManagerImpl(RegionManager pRmgr, FormulaManager pFmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, AbstractionManagerImpl.class);
    logger = pLogger;
    rmgr = pRmgr;
    fmgr = pFmgr;

    absVarToPredicate = new HashMap<Region, AbstractionPredicate>();
    symbVarToPredicate = new HashMap<Formula, AbstractionPredicate>();

    if (useCache) {
      toConcreteCache = new HashMap<Region, Formula>();
    } else {
      toConcreteCache = null;
    }

    new AbstractionPredicatesMBean(); // don't store it, we wouldn't know when to unregister anyway
  }

  @Override
  public AbstractionPredicate makePredicate(Formula atom) {
    Formula var = fmgr.createPredicateVariable(atom);
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      Region absVar = rmgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", absVar,
          "from variable", var, "and atom", atom);
      numberOfPredicates++;

      result = new AbstractionPredicate(absVar, var, atom);
      symbVarToPredicate.put(var, result);
      absVarToPredicate.put(absVar, result);
    }
    return result;
  }

  @Override
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(fmgr.makeFalse());
  }

  @Override
  public AbstractionPredicate getPredicate(Formula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) {
      throw new IllegalArgumentException(var + " seems not to be a formula corresponding to a single predicate variable.");
    }
    return result;
  }

  @Override
  public Formula toConcrete(Region af) {

    Map<Region, Formula> cache;
    if (useCache) {
      cache = toConcreteCache;
    } else {
      cache = new HashMap<Region, Formula>();
    }
    Deque<Region> toProcess = new ArrayDeque<Region>();

    cache.put(rmgr.makeTrue(), fmgr.makeTrue());
    cache.put(rmgr.makeFalse(), fmgr.makeFalse());

    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.peek();
      if (cache.containsKey(n)) {
        toProcess.pop();
        continue;
      }
      boolean childrenDone = true;
      Formula m1 = null;
      Formula m2 = null;

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

        Formula atom = absVarToPredicate.get(var).getSymbolicAtom();

        Formula ite = fmgr.makeIfThenElse(atom, m1, m2);
        cache.put(n, ite);
      }
    }

    Formula result = cache.get(af);
    assert result != null;

    return result;
  }

  @Override
  public Collection<AbstractionPredicate> extractPredicates(Region af) {
    Collection<AbstractionPredicate> vars = new HashSet<AbstractionPredicate>();

    Deque<Region> toProcess = new ArrayDeque<Region>();
    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.pop();

      if(n.equals(rmgr.makeTrue()) || n.equals(rmgr.makeFalse())) {
        vars.add(this.makeFalsePredicate());
        continue;
      }

      if(absVarToPredicate.containsKey(n)) {
        vars.add(absVarToPredicate.get(n));
        continue;
      }

      Triple<Region, Region, Region> parts = rmgr.getIfThenElse(n);
      Region c1 = parts.getSecond();
      Region c2 = parts.getThird();

      if(c1 != null)
        toProcess.push(c1);
      if(c2 != null)
        toProcess.push(c2);

      Region var = parts.getFirst();
      assert(absVarToPredicate.containsKey(var));
      vars.add(absVarToPredicate.get(var));
    }
    return vars;
  }

  @Override
  public AbstractionFormula makeTrueAbstractionFormula(Formula previousBlockFormula) {
    if (previousBlockFormula == null) {
      previousBlockFormula = fmgr.makeTrue();
    }
    return new AbstractionFormula(rmgr.makeTrue(), fmgr.makeTrue(), previousBlockFormula);
  }

  @Override
  public RegionManager getRegionManager() {
    return rmgr;
  }
}
