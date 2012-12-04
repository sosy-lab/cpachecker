/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * This class stores a mapping between abstract regions and the corresponding
 * symbolic formula. It is therefore the bridge between the abstract and the
 * symbolic "worlds".
 * It is also responsible for the creation of {@link AbstractionPredicate}s.
 */
@Options(prefix = "cpa.predicate")
public final class AbstractionManager {

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
  private final Map<Region, AbstractionPredicate> absVarToPredicate = Maps.newHashMap();
  // and the mapping symbolic variable -> predicate
  private final Map<Formula, AbstractionPredicate> symbVarToPredicate = Maps.newHashMap();
  // and the mapping atom -> predicate
  private final Map<Formula, AbstractionPredicate> atomToPredicate = Maps.newHashMap();

  @Option(name = "abs.useCache", description = "use caching of region to formula conversions")
  private boolean useCache = true;

  private final Map<Region, Formula> toConcreteCache;

  public AbstractionManager(RegionManager pRmgr, FormulaManager pFmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, AbstractionManager.class);
    logger = pLogger;
    rmgr = pRmgr;
    fmgr = pFmgr;

    if (useCache) {
      toConcreteCache = new HashMap<Region, Formula>();
    } else {
      toConcreteCache = null;
    }

    new AbstractionPredicatesMBean(); // don't store it, we wouldn't know when to unregister anyway
  }

  /**
   * creates a Predicate from the Boolean symbolic variable (var) and
   * the atom that defines it
   */
  public AbstractionPredicate makePredicate(Formula atom) {
    AbstractionPredicate result = atomToPredicate.get(atom);
    if (result == null) {
      Formula symbVar = fmgr.createPredicateVariable("PRED"+numberOfPredicates++);
      Region absVar = rmgr.createPredicate();

      logger.log(Level.FINEST, "Created predicate", absVar,
          "from variable", symbVar, "and atom", atom);

      result = new AbstractionPredicate(absVar, symbVar, atom);
      symbVarToPredicate.put(symbVar, result);
      absVarToPredicate.put(absVar, result);
      atomToPredicate.put(atom, result);
    }
    return result;
  }

  /**
   * creates a Predicate that represents "false"
   */
  public AbstractionPredicate makeFalsePredicate() {
    return makePredicate(fmgr.makeFalse());
  }

  /**
   * Get predicate corresponding to a variable.
   * @param var A symbolic formula representing the variable. The same formula has to been passed to makePredicate earlier.
   * @return a Predicate
   */
  private AbstractionPredicate getPredicate(Formula var) {
    AbstractionPredicate result = symbVarToPredicate.get(var);
    if (result == null) { throw new IllegalArgumentException(var
        + " seems not to be a formula corresponding to a single predicate variable."); }
    return result;
  }

  /**
   * Given an abstract formula (which is a BDD over the predicates), build
   * its concrete representation (which is a symbolic formula corresponding
   * to the BDD, in which each predicate is replaced with its definition)
   */
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

        AbstractionPredicate pred = absVarToPredicate.get(var);
        assert pred != null;
        Formula atom = pred.getSymbolicAtom();

        Formula ite = fmgr.makeIfThenElse(atom, m1, m2);
        cache.put(n, ite);
      }
    }

    Formula result = cache.get(af);
    assert result != null;

    return result;
  }

  /**
   * checks whether the data region represented by f1
   * is a subset of that represented by f2
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  public boolean entails(Region f1, Region f2) {
    return rmgr.entails(f1, f2);
  }

  public Collection<AbstractionPredicate> extractPredicates(Region af) {
    Collection<AbstractionPredicate> vars = new HashSet<AbstractionPredicate>();

    Deque<Region> toProcess = new ArrayDeque<Region>();
    toProcess.push(af);
    while (!toProcess.isEmpty()) {
      Region n = toProcess.pop();

      if (n.isTrue() || n.isFalse()) {
        vars.add(this.makeFalsePredicate());
        continue;
      }

      AbstractionPredicate pred = absVarToPredicate.get(n);

      if (pred == null) {
        Triple<Region, Region, Region> parts = rmgr.getIfThenElse(n);

        Region var = parts.getFirst();
        pred = absVarToPredicate.get(var);
        assert pred != null;

        Region c1 = parts.getSecond();
        if (c1 != null) {
          toProcess.push(c1);
        }

        Region c2 = parts.getThird();
        if (c2 != null) {
          toProcess.push(c2);
        }
      }

      vars.add(pred);
    }
    return vars;
  }

  public Region buildRegionFromFormula(Formula pF) {
    // expect that pF is uninstantiated
    if(pF.isFalse()){
      return getRegionCreator().makeFalse();
    }

    if(pF.isTrue()){
      return getRegionCreator().makeTrue();
    }

    FormulaOperator op = fmgr.getOperator(pF);

    if (op == null) { return null; }
    switch (op) {
    case ATOM: {
      return atomToPredicate.get(pF).getAbstractVariable();
    }
    case NOT: {
      Formula[] arg = fmgr.getArguments(pF);
      return getRegionCreator().makeNot(buildRegionFromFormula(arg[0]));
    }
    case AND: {
      Formula[] arg = fmgr.getArguments(pF);
      return getRegionCreator().makeAnd(buildRegionFromFormula(arg[0]), buildRegionFromFormula(arg[1]));
    }
    case OR: {
      Formula[] arg = fmgr.getArguments(pF);
      return getRegionCreator().makeOr(buildRegionFromFormula(arg[0]), buildRegionFromFormula(arg[1]));
    }
    case EQUIV: {
      Formula[] arg = fmgr.getArguments(pF);
      return getRegionCreator().makeEqual(buildRegionFromFormula(arg[0]), buildRegionFromFormula(arg[1]));
    }
    case ITE: {
      Formula[] arg = fmgr.getArguments(pF);
      return getRegionCreator().makeIte(buildRegionFromFormula(arg[0]), buildRegionFromFormula(arg[1]),
          buildRegionFromFormula(arg[2]));
    }
    default:
      return null;
    }
  }

  public RegionCreator getRegionCreator() {
    return new RegionCreator();
  }

  public class RegionCreator {

    /**
     * @return a representation of logical truth
     */
    public Region makeTrue() {
      return rmgr.makeTrue();
    }

    /**
     * @return a representation of logical falseness
     */
    public Region makeFalse() {
      return rmgr.makeFalse();
    }

    /**
     * Creates a region representing a negation of the argument
     * @param f an AbstractFormula
     * @return (!f1)
     */
    public Region makeNot(Region f) {
      return rmgr.makeNot(f);
    }

    /**
     * Creates a region representing an AND of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 & f2)
     */
    public Region makeAnd(Region f1, Region f2) {
      return rmgr.makeAnd(f1, f2);
    }

    /**
     * Creates a region representing an OR of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 | f2)
     */
    public Region makeOr(Region f1, Region f2) {
      return rmgr.makeOr(f1, f2);
    }

    /**
     * Creates a region representing an equality (bi-implication) of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (f1 <=> f2)
     */
    public Region makeEqual(Region f1, Region f2) {
      return rmgr.makeEqual(f1, f2);
    }

    /**
     * Creates a region representing an if then else construct of the three arguments
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @param f3 an AbstractFormula
     * @return (if f1 then f2 else f3)
     */
    public Region makeIte(Region f1, Region f2, Region f3) {
      return rmgr.makeIte(f1, f2, f3);
    }

    /**
     * Creates a region representing an existential quantification of the two argument
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return (\exists f2: f1)
     */
    public Region makeExists(Region f1, Region f2) {
      return rmgr.makeExists(f1, f2);
    }

    public Region getPredicate(Formula var) {
      return AbstractionManager.this.getPredicate(var).getAbstractVariable();
    }
  }
}
