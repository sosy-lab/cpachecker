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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.Maps;

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


  // Code for connecting the Java GC and the BDD library GC
  // When a Java object is freed, we need to tell the library.
  // The method with PhantomReferences is a better way then using finalize().
  // In order for this to work, two invariants must hold:
  // - No two BDDRegion objects point to the same BDD instance.
  // - All BDDRegion objects get created by the wrap(BBD) method.
  // For all BDD objects which do not get wrapped in a BDDRegion,
  // free() must be called manually.

  // The reference objects will appear in this queue as soon as their target object was GCed.
  private final ReferenceQueue<BDDRegion> referenceQueue = new ReferenceQueue<BDDRegion>();

  // In this map we store the info which BDD to free after a BDDRegion object was GCed.
  private final Map<PhantomReference<BDDRegion>, BDD> referenceMap = Maps.newIdentityHashMap();

  /**
   * Cleanup all references to BDDs that are no longer needed.
   * We call this method from all public methods, so that this gets done as soon
   * as possible.
   * Usually we would do this in a daemon thread in the background, but the
   * BDD library is not multi-threaded.
   */
  private void cleanupReferences() {
    PhantomReference<? extends BDDRegion> ref;
    while ((ref = (PhantomReference<? extends BDDRegion>)referenceQueue.poll()) != null) {

      BDD bdd = referenceMap.remove(ref);
      assert bdd != null;
      bdd.free();
    }
  }

  /**
   * Wrap a BDD object in a BDDRegion and register it so that we can free the
   * BDD after the BDDRegion was garbage collected.
   * Always use this method, and never the BDDRegion constructor directly.
   */
  private BDDRegion wrap(BDD bdd) {
    BDDRegion region = new BDDRegion(bdd);

    PhantomReference<BDDRegion> ref = new PhantomReference<BDDRegion>(region, referenceQueue);
    referenceMap.put(ref, bdd);

    return region;
  }


  @Override
  public boolean entails(Region pF1, Region pF2) {
      cleanupReferences();

      // check entailment using BDDs: create the BDD representing
      // the implication, and check that it is the TRUE formula
      BDDRegion f1 = (BDDRegion)pF1;
      BDDRegion f2 = (BDDRegion)pF2;
      BDD imp = f1.getBDD().imp(f2.getBDD());

      boolean result = imp.isOne();
      imp.free();
      return result;
  }

  @Override
  public Region makeTrue() {
    cleanupReferences();

    return trueFormula;
  }

  @Override
  public Region makeFalse() {
    cleanupReferences();

    return falseFormula;
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    cleanupReferences();

    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return wrap(f1.getBDD().and(f2.getBDD()));
  }

  @Override
  public Region makeNot(Region pF) {
    cleanupReferences();

    BDDRegion f = (BDDRegion)pF;

    return wrap(f.getBDD().not());
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    cleanupReferences();

    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return wrap(f1.getBDD().or(f2.getBDD()));
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    cleanupReferences();

    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return wrap(f1.getBDD().biimp(f2.getBDD()));
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    cleanupReferences();

    BDDRegion f1 = (BDDRegion)pF1;
    BDDRegion f2 = (BDDRegion)pF2;

    return wrap(f1.getBDD().xor(f2.getBDD()));
  }

  @Override
  public Region createPredicate() {
    cleanupReferences();

    BDD bddVar = createNewVar();

    return wrap(bddVar);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    cleanupReferences();

    BDD f = ((BDDRegion)pF).getBDD();

    Region predicate = wrap(factory.ithVar(f.var()));
    Region fThen = wrap(f.high());
    Region fElse = wrap(f.low());

    return Triple.of(predicate, fThen, fElse);
  }

  @Override
  public Region makeExists(Region pF1, Region pF2) {
    cleanupReferences();

    BDD f1 = ((BDDRegion)pF1).getBDD();
    BDD f2 = ((BDDRegion)pF2).getBDD();

    return wrap(f1.exist(f2));
  }

  public String getVersion() {
    return factory.getVersion();
  }
}
