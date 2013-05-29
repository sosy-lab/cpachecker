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

import java.io.PrintStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * A wrapper for the javabdd (http://javabdd.sf.net) package.
 *
 * This class is not thread-safe, but it could be easily made so by synchronizing
 * the {@link #createNewVar()} method (assuming the BDDFactory is thread-safe).
 */
@Options(prefix = "bdd")
public class BDDRegionManager implements RegionManager {

  private static final Level LOG_LEVEL = Level.FINE;

  @Option(name="package",
      description = "Which BDD package should be used?"
      + "\n- java:  JavaBDD (default, no dependencies, many features)"
      + "\n- cudd:  CUDD (native library required, reordering not supported)"
      + "\n- micro: MicroFactory (maximum number of BDD variables is 1024, slow, but less memory-comsumption)"
      + "\n- buddy: Buddy (native library required)"
      + "\n- cal:   CAL (native library required)"
      + "\n- jdd:   JDD",
      values = {"java", "cudd", "micro", "buddy", "cal", "jdd"})
  // documentation of the packages can be found at source of BDDFactory.init()
  private String bddPackage = "java";

  @Option(description="Initial size of the BDD node table.")
  private int initBddNodeTableSize = 10000;

  @Option(description="Size of the BDD cache if cache ratio is not used.")
  private int bddCacheSize = 1000;

  @Option(description="Size of the BDD cache in relation to the node table size (set to 0 to use fixed BDD cache size).")
  private double bddCacheRatio = 0.1;

  private final LogManager logger;
  private final BDDFactory factory;
  private final Region trueFormula;
  private final Region falseFormula;

  private int nextvar = 0;
  private int varcount = 100;

  private BDDRegionManager(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    factory = BDDFactory.init(bddPackage, initBddNodeTableSize, bddCacheSize);

    // register callbacks for logging
    try {
      Method gcCallback = BDDRegionManager.class.getDeclaredMethod("gcCallback", new Class[]{Integer.class, BDDFactory.GCStats.class});
      gcCallback.setAccessible(true);
      factory.registerGCCallback(this, gcCallback);

      Method resizeCallback = BDDRegionManager.class.getDeclaredMethod("resizeCallback", new Class[]{Integer.class, Integer.class});
      resizeCallback.setAccessible(true);
      factory.registerResizeCallback(this, resizeCallback);

      Method reorderCallback = BDDRegionManager.class.getDeclaredMethod("reorderCallback", new Class[]{Integer.class, BDDFactory.ReorderStats.class});
      reorderCallback.setAccessible(true);
      factory.registerReorderCallback(this, reorderCallback);

      // If we do not log, unregister the handlers to avoid the cost of
      // calling them with reflection.
      // Registering and immediately unregistering prevents the library
      // from printing stuff to stdout.
      if (!logger.wouldBeLogged(LOG_LEVEL)) {
        factory.unregisterGCCallback(this, gcCallback);
        factory.unregisterResizeCallback(this, resizeCallback);
        factory.unregisterReorderCallback(this, reorderCallback);
      }

    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }

    factory.setVarNum(varcount);
    factory.setCacheRatio(bddCacheRatio);

    trueFormula = new BDDRegion(factory.one());
    falseFormula = new BDDRegion(factory.zero());
  }

  /** Instantiate a new BDDRegionManager */
  public static BDDRegionManager getInstance(Configuration config, LogManager logger) throws InvalidConfigurationException {
    return new BDDRegionManager(config, logger);
  }

  @SuppressWarnings("unused")
  private void gcCallback(Integer pre, BDDFactory.GCStats stats) {
    if (logger.wouldBeLogged(LOG_LEVEL)) {
      switch (pre) {
      case 1:
        logger.log(LOG_LEVEL, "Starting BDD Garbage Collection");
        break;
      case 0:
        logger.log(LOG_LEVEL, "Finished BDD", stats);
        break;
      default:
        logger.log(LOG_LEVEL, stats);
      }
    }
  }

  @SuppressWarnings("unused")
  private void resizeCallback(Integer oldSize, Integer newSize) {
    logger.log(LOG_LEVEL, "BDD node table resized from", oldSize, "to", newSize);
  }

  @SuppressWarnings("unused")
  private void reorderCallback(Integer pre, BDDFactory.ReorderStats stats) {
    if (logger.wouldBeLogged(LOG_LEVEL)) {
      switch (pre) {
      case 1:
        logger.log(LOG_LEVEL, "Starting BDD Reordering");
        break;
      case 0:
        logger.log(LOG_LEVEL, "Finished BDD Reordering:", stats);
        break;
      default:
        logger.log(LOG_LEVEL, stats);
      }
    }
  }

  @Override
  public void printStatistics(PrintStream out) {
    try {
      out.println("Number of BDD nodes:                 " + factory.getNodeNum());
      out.println("Size of BDD node table:              " + factory.getNodeTableSize());

      // Cache size is currently always equal to bddCacheSize,
      // unfortunately the library does not update it on cache resizes.
      //out.println("Size of BDD cache:                   " + factory.getCacheSize());

      // Cache stats are disabled in JFactory (CACHESTATS = false)
      //out.println(factory.getCacheStats());

      BDDFactory.GCStats stats = factory.getGCStats();
      out.println("Time for BDD garbage collection:     " + Timer.formatTime(stats.sumtime) + " (in " + stats.num + " runs)");
      // unfortunately, factory.getCacheStats() returns an empty object
      // because statistics collection for the cache is disabled in the library
    } catch (UnsupportedOperationException e) {
      // Not all factories might have all statistics supported.
      // As statistics are not that important, just ignore it.
    }
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
  private final ReferenceQueue<BDDRegion> referenceQueue = new ReferenceQueue<>();

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

    PhantomReference<BDDRegion> ref = new PhantomReference<>(region, referenceQueue);
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
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    cleanupReferences();
    return wrap(unwrap(pF1).ite(unwrap(pF2), unwrap(pF3)));
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
  public Region makeExists(Region pF1, Region... pF2) {
    cleanupReferences();

    // we use id() to get copies of the BDDs, otherwise we would delete them
    BDD f = unwrap(pF2[0]).id();
    for (int i = 1; i < pF2.length; i++) {
      f.andWith(unwrap(pF2[i]).id());
    }
    Region result = wrap(unwrap(pF1).exist(f));
    f.free();

    return result;
  }

  @Override
  public Region fromFormula(BooleanFormula pF, FormulaManagerView fmgr,
      Function<BooleanFormula, Region> atomToRegion) {
    cleanupReferences();

    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    if (bfmgr.isFalse(pF)) {
      return makeFalse();
    }

    if (bfmgr.isTrue(pF)) {
      return makeTrue();
    }

    return wrap(new FormulaToRegionConverter(fmgr, atomToRegion)
                                              .visit(pF));
  }

  /**
   * Class for creating BDDs out of a formula.
   * This class directly uses the BDD objects and their manual reference counting,
   * because for large formulas, the performance impact of creating BDDRegion
   * objects, putting them into the referenceMap and referenceQueue,
   * gc'ing the BDDRegions again, and calling cleanupReferences() would be too big.
   */
  private class FormulaToRegionConverter extends BooleanFormulaManagerView.BooleanFormulaVisitor<BDD> {

    private final Function<BooleanFormula, Region> atomToRegion;

    FormulaToRegionConverter(FormulaManagerView pFmgr,
        Function<BooleanFormula, Region> pAtomToRegion) {
      super(pFmgr);
      atomToRegion = pAtomToRegion;
    }

    @Override
    protected BDD visitTrue() {
      return factory.one();
    }

    @Override
    protected BDD visitFalse() {
      return factory.zero();
    }

    @Override
    public BDD visitAtom(BooleanFormula pAtom) {
      return ((BDDRegion)atomToRegion.apply(pAtom)).getBDD().id();
    }

    @Override
    public BDD visitNot(BooleanFormula pOperand) {
      BDD operand = visit(pOperand);
      BDD result = operand.not();
      operand.free();
      return result;
    }

    private BDD visitBinary(BooleanFormula pOperand1, BooleanFormula pOperand2,
        BDDFactory.BDDOp operator) {

      BDD operand1 = visit(pOperand1);
      BDD operand2 = visit(pOperand2);

      // optimization: applyWith() destroys arg0 and arg1,
      // but this is ok, because we would free them otherwise anyway
      return operand1.applyWith(operand2, operator);
    }

    @Override
    public BDD visitAnd(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return visitBinary(pOperand1, pOperand2, BDDFactory.and);
    }

    @Override
    public BDD visitOr(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return visitBinary(pOperand1, pOperand2, BDDFactory.or);
    }

    @Override
    public BDD visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return visitBinary(pOperand1, pOperand2, BDDFactory.biimp);
    }

    @Override
    public BDD visitIfThenElse(BooleanFormula pCondition,
        BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      BDD condition = visit(pCondition);
      BDD thenBDD = visit(pThenFormula);
      BDD elseBDD = visit(pElseFormula);

      BDD result = condition.ite(thenBDD, elseBDD);

      condition.free();
      thenBDD.free();
      elseBDD.free();
      return result;
    }
  }


  public String getVersion() {
    return factory.getVersion();
  }
}
