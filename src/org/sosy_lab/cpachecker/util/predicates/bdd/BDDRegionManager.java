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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

import com.google.common.collect.Maps;

/**
 * A wrapper for the javabdd (http://javabdd.sf.net) package.
 *
 * This class is not thread-safe, but it could be easily made so by synchronizing
 * the {@link #createNewVar()} method.
 */
@Options(prefix = "bddregionmanager")
public class BDDRegionManager implements RegionManager {

  @Option(name = "package", description = "which bdd-package should be used? "
      + "\n- cudd:  CUDD-library, reordering not supported"
      + "\n- java:  JFactory, fallback for all other packages"
      + "\n- micro: MicroFactory, maximum number of BDD variables is 1024,"
      + "\n         slow, but less memory-comsumption.",
      values = { "cudd", "java", "micro" })
  // documentation of the packages can be found at source of BDDFactory.init()
  private String BDD_PACKAGE = "cudd";

  private final BDDFactory factory;
  private final Region trueFormula;
  private final Region falseFormula;

  private static int nextvar = 0;
  private static int varcount = 100;

  private static BDDRegionManager instance;

  private BDDRegionManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    factory = BDDFactory.init(BDD_PACKAGE, 10000, 1000);
    factory.setVarNum(varcount);
    trueFormula = new BDDRegion(factory.one());
    falseFormula = new BDDRegion(factory.zero());
  }

  /** this method will return the same object at any call. the very first given
   *  configuration will be used, each other configuration will be ignored. */
  public static BDDRegionManager getInstance(Configuration config) throws InvalidConfigurationException {
    if (instance == null) {
      instance = new BDDRegionManager(config);
    }
    return instance;
  }

  private BDD createNewVar() {
    if (nextvar >= varcount) {
      varcount *= 1.5;
      factory.setVarNum(varcount);
    }
    BDD ret = factory.ithVar(nextvar++);

    return ret;
  }

  @Override
  public BDDRegion createPredicate() {
    cleanupReferences();
    return wrap(createNewVar());
  }

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

  private BDD unwrap(Region region) {
    return ((BDDRegion) region).getBDD();
  }

  @Override
  public boolean entails(Region pF1, Region pF2) {
    cleanupReferences();

    // check entailment using BDDs: create the BDD representing
    // the implication, and check that it is the TRUE formula
    BDD imp = unwrap(pF1).imp(unwrap(pF2));

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

    return wrap(unwrap(pF1).and(unwrap(pF2)));
  }

  @Override
  public Region makeNot(Region pF) {
    cleanupReferences();

    return wrap(unwrap(pF).not());
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    cleanupReferences();

    return wrap(unwrap(pF1).or(unwrap(pF2)));
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    cleanupReferences();

    return wrap(unwrap(pF1).biimp(unwrap(pF2)));
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    cleanupReferences();

    return wrap(unwrap(pF1).xor(unwrap(pF2)));
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    cleanupReferences();

    BDD f = unwrap(pF);

    Region predicate = wrap(factory.ithVar(f.var()));
    Region fThen = wrap(f.high());
    Region fElse = wrap(f.low());

    return Triple.of(predicate, fThen, fElse);
  }

  @Override
  public Region makeExists(Region pF1, Region pF2) {
    cleanupReferences();

    return wrap(unwrap(pF1).exist(unwrap(pF2)));
  }

  public int getNumberOfNodes() {
    return factory.getNodeNum();
  }

  public String getVersion() {
    return factory.getVersion();
  }

  @Override
  public String getStatistics() {
    return "Number of nodes:              " + getNumberOfNodes() +
         "\nTablesize:                    " + factory.getNodeTableSize() + "\n";
  }
}
