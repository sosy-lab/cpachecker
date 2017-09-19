/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.io.PrintStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.PredicateOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.BooleanFormulaVisitor;
/**
 * A wrapper for the javabdd (http://javabdd.sf.net) package.
 *
 * This class is not thread-safe, but it could be easily made so by synchronizing
 * the {@link #createNewVar()} method (assuming the BDDFactory is thread-safe).
 */
@Options(prefix = "bdd.javabdd")
class JavaBDDRegionManager implements RegionManager {
  private static final Level LOG_LEVEL = Level.FINE;

  // Statistics
  private final StatInt cleanupQueueSize = new StatInt(StatKind.AVG, "Size of BDD node cleanup queue");
  private final StatTimer cleanupTimer = new StatTimer("Time for BDD node cleanup");
  private final LogManager logger;
  private final BDDFactory factory;
  private final Region trueFormula;
  private final Region falseFormula;
  // The reference objects will appear in this queue as soon as their target object was GCed.
  private final ReferenceQueue<JavaBDDRegion> referenceQueue =
      new ReferenceQueue<>();
  // In this map we store the info which BDD to free after a JavaBDDRegion object was GCed.
  private final Map<Reference<? extends JavaBDDRegion>, BDD> referenceMap = Maps
      .newIdentityHashMap();

  @Option(secure = true, description = "Initial size of the BDD node table in percentage of available Java heap memory (only used if initTableSize is 0).")
  private double initTableRatio = 0.001;

  @Option(secure = true, description = "Initial size of the BDD node table, use 0 for size based on initTableRatio.")
  @IntegerOption(min = 0)
  private int initTableSize = 0;

  @Option(secure = true, description = "Initial size of the BDD cache, use 0 for cacheRatio*initTableSize.")
  @IntegerOption(min = 0)
  private int cacheSize = 0;

  @Option(secure = true,
      description = "Size of the BDD cache in relation to the node table size (set to 0 to use fixed BDD cache size).")
  private double cacheRatio = 0.1;
  private int nextvar = 0;
  private int varcount = 100;

  JavaBDDRegionManager(String bddPackage, Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    if (initTableRatio <= 0 || initTableRatio >= 1) {
      throw new InvalidConfigurationException("Invalid value " + initTableRatio
          + " for option bdd.javabdd.initTableRatio, needs to be between 0 and 1.");
    }
    if (initTableSize == 0) {
      // JFactory uses 5 ints of 4 byte sizes for each entry in the BDD table
      double size = Runtime.getRuntime().maxMemory() * initTableRatio / 5 / 4;
      initTableSize = (size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)size;
      logger.log(Level.CONFIG, "Setting value of bdd.javabdd.initTableSize to", initTableSize);
    }

    if (cacheRatio < 0) {
      throw new InvalidConfigurationException("Invalid value " + cacheRatio
          + " for option bdd.javabdd.cacheRatio, cannot be negative.");
    }
    if (cacheSize == 0) {
      cacheSize = (int)(initTableSize * cacheRatio);
    }
    factory =
        BDDFactory.init(bddPackage.toLowerCase(), initTableSize, cacheSize);

    // register callbacks for logging
    try {
      Method gcCallback =
          JavaBDDRegionManager.class.getDeclaredMethod("gcCallback",
              Integer.class, BDDFactory.GCStats.class);
      gcCallback.setAccessible(true);
      factory.registerGCCallback(this, gcCallback);

      Method resizeCallback =
          JavaBDDRegionManager.class.getDeclaredMethod("resizeCallback",
              Integer.class, Integer.class);
      resizeCallback.setAccessible(true);
      factory.registerResizeCallback(this, resizeCallback);

      Method reorderCallback =
          JavaBDDRegionManager.class
              .getDeclaredMethod("reorderCallback", Integer.class,
                  BDDFactory.ReorderStats.class);
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
    factory.setCacheRatio(cacheRatio);

    trueFormula = new JavaBDDRegion(factory.one());
    falseFormula = new JavaBDDRegion(factory.zero());
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
    logger.log(LOG_LEVEL, "BDD node table resized from", oldSize, "to",
        newSize);
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
      BDDFactory.GCStats stats = factory.getGCStats();
      int cacheSize = readCacheSize();

      writingStatisticsTo(out)
          .put("Number of BDD nodes", factory.getNodeNum())
          .put("Size of BDD node table", factory.getNodeTableSize())
          .putIf(cacheSize >= 0, "Size of BDD cache", cacheSize)
          .put(cleanupQueueSize)
          .put(cleanupTimer)
          .put(
              "Time for BDD garbage collection",
              TimeSpan.ofMillis(stats.sumtime).formatAs(SECONDS)
                  + " (in " + stats.num + " runs)");

      // Cache stats are disabled in JFactory (CACHESTATS = false)
      // out.println(factory.getCacheStats());
    } catch (UnsupportedOperationException e) {
      // Not all factories might have all statistics supported.
      // As statistics are not that important, just ignore it.
    }
  }

  /**
   * Return the current size of the cache of the BDD library.
   * Returns -1 if value cannot be read.
   */
  private int readCacheSize() {
    if (factory instanceof JFactory) {
      // Unfortunately JFactory does not update its reported size on cache resizes.
      try {
        Field cacheField = JFactory.class.getDeclaredField("applycache");
        cacheField.setAccessible(true);
        Object cache = cacheField.get(factory);
        if (cache != null) {
          Field tableField = cache.getClass().getDeclaredField("table");
          tableField.setAccessible(true);
          Object table = tableField.get(cache);
          if (table instanceof Object[]) {
            return ((Object[])table).length;
          }
        }
      } catch (ReflectiveOperationException | SecurityException e) {
        logger.logDebugException(e, "Could not access cache field of JFactory for statistics");
      }
      return -1;
    }
    return factory.getCacheSize();
  }

  // Code for connecting the Java GC and the BDD library GC
  // When a Java object is freed, we need to tell the library.
  // The method with PhantomReferences is a better way then using finalize().
  // In order for this to work, two invariants must hold:
  // - No two JavaBDDRegion objects point to the same BDD instance.
  // - All JavaBDDRegion objects get created by the wrap(BBD) method.
  // For all BDD objects which do not get wrapped in a JavaBDDRegion,
  // free() must be called manually.

  private BDD createNewVar() {
    if (nextvar >= varcount) {
      varcount = (int) (varcount * 1.5);
      factory.setVarNum(varcount);
    }
    BDD ret = factory.ithVar(nextvar++);

    factory.printOrder();

    return ret;
  }

  @Override
  public JavaBDDRegion createPredicate() {
    cleanupReferences();
    return wrap(createNewVar());
  }

  /**
   * Cleanup all references to BDDs that are no longer needed.
   * We call this method from all public methods, so that this gets done as soon
   * as possible.
   * Usually we would do this in a daemon thread in the background, but the
   * BDD library is not multi-threaded.
   */
  private void cleanupReferences() {
    cleanupTimer.start();
    try {
      int count = 0;
      Reference<? extends JavaBDDRegion> ref;
      while ((ref = referenceQueue
              .poll()) != null) {
        count++;

        BDD bdd = referenceMap.remove(ref);
        assert bdd != null;
        bdd.free();
      }
      cleanupQueueSize.setNextValue(count);
    } finally {
      cleanupTimer.stop();
    }
  }

  /**
   * Wrap a BDD object in a JavaBDDRegion and register it so that we can free the
   * BDD after the JavaBDDRegion was garbage collected.
   * Always use this method, and never the JavaBDDRegion constructor directly.
   */
  private JavaBDDRegion wrap(BDD bdd) {
    JavaBDDRegion region = new JavaBDDRegion(bdd);

    PhantomReference<JavaBDDRegion> ref = new PhantomReference<>(region, referenceQueue);
    referenceMap.put(ref, bdd);

    return region;
  }

  private BDD unwrap(Region region) {
    return ((JavaBDDRegion) region).getBDD();
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

    if (pF2.length == 0 || pF1.isTrue() || pF1.isFalse()) {
      return pF1;
    }

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
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return new BDDRegionBuilder(pShutdownNotifier);
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

    try (FormulaToRegionConverter converter =
             new FormulaToRegionConverter(fmgr, atomToRegion)) {
      return wrap(bfmgr.visit(pF, converter));
    }
  }

  @Override
  public String getVersion() {
    return factory.getVersion();
  }

  @Override
  public void setVarOrder(ArrayList<Integer> pVarOrder) {
    int[] order = new int[varcount];
    for (int i = 0; i < order.length; i++) {
      if (i < pVarOrder.size()) {
        order[i] = pVarOrder.get(i);
      } else {
        order[i] = i;
      }
    }
    factory.setVarOrder(order);
  }

  @Override
  public void reorder(PredicateOrderingStrategy strategy) {
    switch (strategy) {
      case FRAMEWORK_RANDOM:
        factory.reorder(BDDFactory.REORDER_RANDOM);
        break;
      case FRAMEWORK_SIFT:
        factory.reorder(BDDFactory.REORDER_SIFT);
        break;
      case FRAMEWORK_SIFTITE:
        factory.reorder(BDDFactory.REORDER_SIFTITE);
        break;
      case FRAMEWORK_WIN2:
        factory.reorder(BDDFactory.REORDER_WIN2);
        break;
      case FRAMEWORK_WIN2ITE:
        factory.reorder(BDDFactory.REORDER_WIN2ITE);
        break;
      case FRAMEWORK_WIN3:
        factory.reorder(BDDFactory.REORDER_WIN3);
        break;
      case FRAMEWORK_WIN3ITE:
        factory.reorder(BDDFactory.REORDER_WIN3ITE);
        break;
      default:
        break;
    }
  }

  private class BDDRegionBuilder implements RegionBuilder {

    private final ShutdownNotifier shutdownNotifier;
    // Invariants:
    // cubes contains a number of BDDs, whose disjunction makes up the result.
    // cubes may also contain null values, which are to be ignored,
    // but there is always at least one non-null value (if the list is not empty).
    // The cube at index i is one built from i+1 models.
    // When inserting, we find the left-most place in the list where we can insert.
    // If the list is empty, we just add the cube at position 0.
    // If this position is filled, we take the new cube and the cube from position 0,
    // disjunct them and try storing the result at position 1,
    // iteratively increasing the position.
    // This is used to create balanced disjunctions
    // instead of using a single growing BDD,
    // while at the same time limiting the number of stored BDDs
    // (log(numOfCubes) many).
    private final List<BDD> cubes = new ArrayList<>();
    // Invariant: currentCube and everything in cubes
    // is allowed to be mutated/destroyed, i.e., there is no other reference to it.
    private BDD currentCube = null;

    private BDDRegionBuilder(ShutdownNotifier pShutdownNotifier) {
      shutdownNotifier = pShutdownNotifier;
    }

    @Override
    public void startNewConjunction() {
      checkState(currentCube == null);
      // one() creates new BDD
      currentCube = factory.one(); // true
    }

    @Override
    public void addPositiveRegion(Region r) {
      checkState(currentCube != null);
      // call id() for copy
      currentCube.andWith(((JavaBDDRegion)r).getBDD().id());
    }

    @Override
    public void addNegativeRegion(Region r) {
      checkState(currentCube != null);
      // not() creates new BDD
      currentCube.andWith(((JavaBDDRegion)r).getBDD().not());
    }

    @Override
    public void finishConjunction() {
      checkState(currentCube != null);

      for (int i = 0; i < cubes.size(); i++) {
        BDD cubeAtI = cubes.get(i);

        if (cubeAtI == null) {
          cubes.set(i, currentCube);
          currentCube = null;
          return;
        } else {
          currentCube.orWith(cubeAtI);
          cubes.set(i, null);
        }
      }

      if (currentCube != null) {
        cubes.add(currentCube);
        currentCube = null;
      }
    }

    @Override
    public Region getResult() throws InterruptedException {
      checkState(currentCube == null);
      if (cubes.isEmpty()) {
        return falseFormula;
      } else {
        buildBalancedOr();
        // call id() for copy
        return wrap(Iterables.getOnlyElement(cubes).id());
      }
    }

    private void buildBalancedOr() throws InterruptedException {
      BDD result = factory.zero(); // false

      for (BDD cube : cubes) {
        if (cube != null) {
          shutdownNotifier.shutdownIfNecessary();
          result.orWith(cube);
        }
      }
      cubes.clear();
      cubes.add(result);
      assert (cubes.size() == 1);
    }

    @Override
    public void close() {
      checkState(currentCube == null);
      for (BDD bdd : cubes) {
        bdd.free();
      }
      cubes.clear();
    }
  }

  /**
   * Class for creating BDDs out of a formula. This class directly uses the BDD objects and their manual reference
   * counting, because for large formulas, the
   * performance impact of creating JavaBDDRegion objects, putting them into the referenceMap and referenceQueue, gc'ing
   * the BDDRegions again, and calling
   * cleanupReferences() would be too big.
   */
  private class FormulaToRegionConverter
      implements AutoCloseable,
                BooleanFormulaVisitor<BDD> {

    private final Function<BooleanFormula, Region> atomToRegion;
    private final Map<BooleanFormula, BDD> cache = new HashMap<>();
    private final BooleanFormulaManager bfmgr;

    FormulaToRegionConverter(FormulaManagerView pFmgr,
        Function<BooleanFormula, Region> pAtomToRegion) {
      atomToRegion = pAtomToRegion;
      bfmgr = pFmgr.getBooleanFormulaManager();
    }

    @Override
    public BDD visitConstant(boolean value) {
      return value ? factory.one() : factory.zero();
    }

    @Override
    public BDD visitBoundVar(BooleanFormula var, int deBruijnIdx) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BDD visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> decl) {
      return ((JavaBDDRegion)atomToRegion.apply(pAtom)).getBDD().id();
    }

    private BDD convert(BooleanFormula pOperand) {
      BDD operand = cache.get(pOperand);
      if (operand == null) {
        operand = bfmgr.visit(pOperand, this);
        cache.put(pOperand, operand);
      }
      return operand.id(); // copy BDD so the one in the cache won't be consumed
    }

    @Override
    public void close() {
      for (BDD bdd : cache.values()) {
        bdd.free();
      }
      cache.clear();
    }

    @Override
    public BDD visitNot(BooleanFormula pOperand) {
      BDD operand = convert(pOperand);
      BDD result = operand.not();
      operand.free();
      return result;
    }

    private BDD visitBinary(BooleanFormula pOperand1,
        BooleanFormula pOperand2, BDDFactory.BDDOp operator) {

      BDD operand1 = convert(pOperand1);
      BDD operand2 = convert(pOperand2);

      // optimization: applyWith() destroys arg0 and arg1,
      // but this is ok, because we would free them otherwise anyway
      return operand1.applyWith(operand2, operator);
    }

    private BDD visitMulti(BDDFactory.BDDOp operator, List<BooleanFormula> pOperands) {
      checkArgument(pOperands.size() >= 2);

      BDD result = convert(pOperands.get(0));
      for (int i = 1; i < pOperands.size(); i++) {
        // optimization: applyWith() destroys arg0 and arg1,
        // but this is ok, because we would free them otherwise anyway
        result = result.applyWith(convert(pOperands.get(i)), operator);
      }

      return result;
    }

    @Override
    public BDD visitAnd(List<BooleanFormula> pOperands) {
      return visitMulti(BDDFactory.and, pOperands);
    }

    @Override
    public BDD visitOr(List<BooleanFormula> pOperands) {
      return visitMulti(BDDFactory.or, pOperands);
    }

    @Override
    public BDD visitXor(BooleanFormula operand1, BooleanFormula operand2) {
      return visitBinary(operand1, operand2, BDDFactory.xor);
    }

    @Override
    public BDD visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return visitBinary(pOperand1, pOperand2, BDDFactory.biimp);
    }

    @Override
    public BDD visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return visitBinary(pOperand1, pOperand2, BDDFactory.imp);
    }

    @Override
    public BDD visitIfThenElse(
        BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      BDD condition = convert(pCondition);
      BDD thenBDD = convert(pThenFormula);
      BDD elseBDD = convert(pElseFormula);

      BDD result = condition.ite(thenBDD, elseBDD);

      condition.free();
      thenBDD.free();
      elseBDD.free();
      return result;
    }

    @Override
    public BDD visitQuantifier(Quantifier q, BooleanFormula quantifiedAST, List<Formula> args,
                               BooleanFormula pBody) {
      throw new UnsupportedOperationException();
    }
  }
}
