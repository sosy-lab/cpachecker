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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

/**
 * A wrapper for the javabdd (http://javabdd.sf.net) package.
 *
 * This class is not thread-safe, but it could be easily made so by synchronizing
 * the {@link #createNewVar()} method.
 */
public class BDDRegionManager implements RegionManager {

  // static because init() may be called only once!
  private static final String BDD_PACKAGE = "cudd";
  private static final BDDFactory factory = BDDFactory.init(BDD_PACKAGE, 10000, 1000);

  private static final Region trueFormula = new BDDRegion(factory.one());
  private static final Region falseFormula = new BDDRegion(factory.zero());

  private static int nextvar = 0;
  private static int varcount = 100;
  {
    factory.setVarNum(varcount);
  }

  private static BDD createNewVar() {
    if (nextvar >= varcount) {
      varcount *= 1.5;
      factory.setVarNum(varcount);
    }
    BDD ret = factory.ithVar(nextvar++);

    return ret;
  }

  private static RegionManager instance = new BDDRegionManager();

  public static RegionManager getInstance() { return instance; }

  @Override
  public boolean entails(Region pF1, Region pF2) {
      // check entailment using BDDs: create the BDD representing
      // the implication, and check that it is the TRUE formula
      BDDRegion f1 = (BDDRegion)pF1;
      BDDRegion f2 = (BDDRegion)pF2;
      BDD imp = f1.getBDD().imp(f2.getBDD());
      return imp.isOne();
  }

  @Override
  public Region makeTrue() {
    return trueFormula;
  }

  @Override
  public Region makeFalse() {
    return falseFormula;
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return new BDDRegion(f1.getBDD().and(f2.getBDD()));
  }

  @Override
  public Region makeNot(Region pF) {
    BDDRegion f = (BDDRegion)pF;

    return new BDDRegion(f.getBDD().not());
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return new BDDRegion(f1.getBDD().or(f2.getBDD()));
  }

  @Override
  public Region createPredicate() {
    BDD bddVar = createNewVar();

    return new BDDRegion(bddVar);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    BDD f = ((BDDRegion)pF).getBDD();

    BDDRegion predicate = new BDDRegion(factory.ithVar(f.var()));
    BDDRegion fThen = new BDDRegion(f.high());
    BDDRegion fElse = new BDDRegion(f.low());

    return new Triple<Region, Region, Region>(predicate, fThen, fElse);
  }

  @Override
  public Region makeExists(Region pF1, Region pF2) {
    BDD f1 = ((BDDRegion)pF1).getBDD();
    BDD f2 = ((BDDRegion)pF2).getBDD();

    return new BDDRegion(f1.exist(f2));
  }
}
