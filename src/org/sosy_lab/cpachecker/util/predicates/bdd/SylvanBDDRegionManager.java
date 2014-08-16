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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static jsylvan.JSylvan.*;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jsylvan.JSylvan;

import org.sosy_lab.common.Triple;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

/**
 * A wrapper for the Sylvan (http://fmt.ewi.utwente.nl/tools/sylvan/) parallel BDD package,
 * using the Java bindings JSylvan (https://github.com/trolando/jsylvan).
 */
@Options(prefix = "bdd.sylvan")
class SylvanBDDRegionManager implements RegionManager {

  private static final int SYLVAN_MAX_THREADS = 64;


  static {
    NativeLibraries.loadLibrary("sylvan");
  }

  @Option(description="Log2 size of the BDD node table.")
  @IntegerOption(min=1)
  private int tableSize = 26;

  @Option(description="Log2 size of the BDD cache.")
  @IntegerOption(min=1)
  private int cacheSize = 24;

  @Option(description="Granularity of the Sylvan BDD operations cache (recommended values 4-8).")
  @IntegerOption(min=1)
  private int cacheGranularity = 4;

  @Option(description="Number of worker threads, 0 for automatic.")
  @IntegerOption(min=0)
  private int threads = 0;

  // Statistics
  private final StatTimer cleanupTimer = new StatTimer("Time for BDD cleanup after GC");

  private final Region trueFormula;
  private final Region falseFormula;

  private int nextvar = 0;

  public SylvanBDDRegionManager(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    if (threads == 0) {
      threads = Runtime.getRuntime().availableProcessors();
    }
    if (threads > SYLVAN_MAX_THREADS) {
      pLogger.logf(Level.WARNING, "Sylvan does not support %d threads, using %d threads.", threads, SYLVAN_MAX_THREADS);
      threads = SYLVAN_MAX_THREADS;
    }
    JSylvan.initialize(threads, 100000, tableSize, cacheSize, cacheGranularity);

    trueFormula = new SylvanBDDRegion(JSylvan.getTrue());
    falseFormula = new SylvanBDDRegion(JSylvan.getFalse());

    Threads.newThread(new Runnable() {
        @Override
        public void run() {
          // We pass all references explicitly to a static method
          // in order to not leak the reference to the SylvanBDDRegionManager
          // from within its constructor to a separate thread
          // (this is not thread safe in Java).
          cleanupReferences(referenceQueue, referenceMap, cleanupTimer);
        }
      }, "BDD cleanup thread", true).start();
  }

  /** Instantiate a new SylvanBDDRegionManager */
  public static SylvanBDDRegionManager getInstance(Configuration config, LogManager logger) throws InvalidConfigurationException {
    return new SylvanBDDRegionManager(config, logger);
  }

  @Override
  public void printStatistics(PrintStream out) {
    synchronized (cleanupTimer) {
      writingStatisticsTo(out)
        .putIf(cleanupTimer.getUpdateCount() > 0,
               "Number of BDD freed by GC", cleanupTimer.getUpdateCount())
        .putIfUpdatedAtLeastOnce(cleanupTimer)
        ;
    }
  }

  @Override
  public SylvanBDDRegion createPredicate() {
    return wrap(JSylvan.makeVar(nextvar++));
  }

  // Code for connecting the Java GC and the BDD library GC
  // When a Java object is freed, we need to tell the library.
  // The method with PhantomReferences is a better way then using finalize().
  // In order for this to work, two invariants must hold:
  // - No two SylvanBDDRegion objects point to the same BDD instance.
  // - All SylvanBDDRegion objects get created by the wrap(BBD) method.
  // For all BDD objects which do not get wrapped in a SylvanBDDRegion,
  // free() must be called manually.

  // The reference objects will appear in this queue as soon as their target object was GCed.
  private final ReferenceQueue<SylvanBDDRegion> referenceQueue = new ReferenceQueue<>();

  // In this map we store the info which BDD to free after a SylvanBDDRegion object was GCed.
  // Needs to be concurrent because we access it from two threads,
  // and we don't want synchronized blocks in the main thread.
  private final Map<PhantomReference<SylvanBDDRegion>, Long> referenceMap = Maps.newConcurrentMap();

  /**
   * Cleanup all references to BDDs that are no longer needed,
   * after the GC notified us about the fact that it freed a SylvanBDDRegion object.
   * This method runs in a separate thread infinitely.
   */
  private static void cleanupReferences(
      final ReferenceQueue<SylvanBDDRegion> referenceQueue,
      final Map<PhantomReference<SylvanBDDRegion>, Long> referenceMap,
      final StatTimer cleanupTimer) {

    try {
      while (true) {
        PhantomReference<? extends SylvanBDDRegion> ref =
            (PhantomReference<? extends SylvanBDDRegion>)referenceQueue.remove();

        // It would be faster to have a thread-safe timer instead of synchronized.
        // However, the lock is uncontended, and thus probably quite fast
        // (and it does not hurt the main thread).
        synchronized (cleanupTimer) {
          cleanupTimer.start();
          long bdd = referenceMap.remove(ref);
          JSylvan.deref(bdd);
          cleanupTimer.stop();
        }
      }
    } catch (InterruptedException e) {
      // do nothing, we just let this thread terminate
    }
  }

  /**
   * Wrap a BDD object in a SylvanBDDRegion and register it so that we can free the
   * BDD after the SylvanBDDRegion was garbage collected.
   * Always use this method, and never the SylvanBDDRegion constructor directly.
   */
  private SylvanBDDRegion wrap(long bdd) {
    JSylvan.ref(bdd);
    SylvanBDDRegion region = new SylvanBDDRegion(bdd);

    PhantomReference<SylvanBDDRegion> ref = new PhantomReference<>(region, referenceQueue);
    referenceMap.put(ref, bdd);

    return region;
  }

  private long unwrap(Region region) {
    return ((SylvanBDDRegion) region).getBDD();
  }

  @Override
  public boolean entails(Region pF1, Region pF2) {
    // check entailment using BDDs: create the BDD representing
    // the implication, and check that it is the TRUE formula
    long imp = JSylvan.makeImplies(unwrap(pF1), unwrap(pF2));

    return imp == JSylvan.getTrue();
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
    return wrap(JSylvan.makeAnd(unwrap(pF1), unwrap(pF2)));
  }

  @Override
  public Region makeNot(Region pF) {
    return wrap(JSylvan.makeNot(unwrap(pF)));
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    return wrap(JSylvan.makeOr(unwrap(pF1), unwrap(pF2)));
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    return wrap(JSylvan.makeEquals(unwrap(pF1), unwrap(pF2)));
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    return wrap(JSylvan.makeNotEquals(unwrap(pF1), unwrap(pF2)));
  }


  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    return wrap(JSylvan.makeIte(unwrap(pF1), unwrap(pF2), unwrap(pF3)));
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    long f = unwrap(pF);

    Region predicate = wrap(JSylvan.getIf(f));
    Region fThen = wrap(JSylvan.getThen(f));
    Region fElse = wrap(JSylvan.getElse(f));

    return Triple.of(predicate, fThen, fElse);
  }

  @Override
  public Region makeExists(Region pF1, Region... pF2) {
    if (pF2.length == 0) {
      return pF1;
    }

    int[] vars = new int[pF2.length];
    for (int i = 0; i < pF2.length; i++) {
      vars[i] = JSylvan.getVar(unwrap(pF2[i]));
    }
    long varSet = ref(JSylvan.makeSet(vars));
    Region result = wrap(JSylvan.makeExists(unwrap(pF1), varSet));
    deref(varSet);
    return result;
  }

  @Override
  public Set<Region> extractPredicates(Region pF) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return new SylvanBDDRegionBuilder();
  }

  private class SylvanBDDRegionBuilder implements RegionBuilder {

    // Invariant: currentCube and everything in cubes
    // is ref'ed and allowed to be deref'ed.
    private long currentCube = -1;

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
    private final List<Long> cubes = new ArrayList<>();

    @Override
    public void startNewConjunction() {
      checkState(currentCube == -1);
      currentCube = ref(JSylvan.getTrue());
    }

    @Override
    public void addPositiveRegion(Region r) {
      checkState(currentCube != -1);
      long result = ref(JSylvan.makeAnd(currentCube, unwrap(r)));
      deref(currentCube);
      currentCube = result;
    }

    @Override
    public void addNegativeRegion(Region r) {
      checkState(currentCube != -1);
      long negative = ref(JSylvan.makeNot(unwrap(r)));
      long result = ref(JSylvan.makeAnd(currentCube, negative));
      deref(negative);
      deref(currentCube);
      currentCube = result;
    }

    @Override
    public void finishConjunction() {
      checkState(currentCube != -1);

      for (int i = 0; i < cubes.size(); i++) {
        Long cubeAtI = cubes.get(i);

        if (cubeAtI == null) {
          cubes.set(i, currentCube);
          currentCube = -1;
          return;
        } else {
          long result = ref(JSylvan.makeOr(currentCube, cubeAtI));
          deref(currentCube);
          deref(cubeAtI);
          currentCube = result;
          cubes.set(i, null);
        }
      }

      if (currentCube != -1) {
        cubes.add(currentCube);
        currentCube = -1;
      }
    }

    @Override
    public Region getResult() throws InterruptedException {
      checkState(currentCube == -1);
      if (cubes.isEmpty()) {
        return falseFormula;
      } else {

        long[] clauses = Longs.toArray(from(cubes).filter(Predicates.notNull()).toList());
        long result = ref(makeUnionPar(clauses));
        for (long bdd : clauses) {
          deref(bdd);
        }
        cubes.clear();

        cubes.add(result);
        return wrap(result);
      }
    }

    @Override
    public void close() {
      checkState(currentCube == -1);
      for (long bdd : cubes) {
        deref(bdd);
      }
      cubes.clear();
    }
  }

  @Override
  public Region fromFormula(BooleanFormula pF, FormulaManagerView fmgr,
      Function<BooleanFormula, Region> atomToRegion) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    if (bfmgr.isFalse(pF)) {
      return makeFalse();
    }

    if (bfmgr.isTrue(pF)) {
      return makeTrue();
    }


    try (FormulaToRegionConverter converter = new FormulaToRegionConverter(fmgr, atomToRegion)) {
      return wrap(converter.visit(pF));
    }
  }

  /**
   * Class for creating BDDs out of a formula.
   * This class directly uses the BDD objects and their manual reference counting,
   * because for large formulas, the performance impact of creating SylvanBDDRegion
   * objects, putting them into the referenceMap and referenceQueue,
   * gc'ing the SylvanBDDRegions again, and freeing them in cleanupReferences() would be too big.
   *
   * All visit* methods from this class return methods that have not been ref'ed.
   */
  private class FormulaToRegionConverter extends BooleanFormulaManagerView.BooleanFormulaVisitor<Long>
                                         implements AutoCloseable {

    private final Function<BooleanFormula, Region> atomToRegion;

    // All BDDs in cache are ref'ed and are deref'ed in the close() method.
    private final Map<BooleanFormula, Long> cache = new HashMap<>();

    FormulaToRegionConverter(FormulaManagerView pFmgr,
        Function<BooleanFormula, Region> pAtomToRegion) {
      super(pFmgr);
      atomToRegion = pAtomToRegion;
    }

    @Override
    protected Long visitTrue() {
      return JSylvan.getTrue();
    }

    @Override
    protected Long visitFalse() {
      return JSylvan.getFalse();
    }

    @Override
    public Long visitAtom(BooleanFormula pAtom) {
      return unwrap(atomToRegion.apply(pAtom));
    }

    // Convert one BooleanFormula (recursively)
    // and return a ref'ed result that is also put in the cache.
    private long convert(BooleanFormula pOperand) {
      Long operand = cache.get(pOperand);
      if (operand == null) {
        operand = ref(visit(pOperand));
        cache.put(pOperand, operand);
      }
      return operand;
    }

    @Override
    public void close() {
      for (long bdd : cache.values()) {
        deref(bdd);
      }
      cache.clear();
    }

    @Override
    public Long visitNot(BooleanFormula pOperand) {
      return JSylvan.makeNot(convert(pOperand));
    }

    @Override
    public Long visitAnd(BooleanFormula... pOperands) {
      long result = JSylvan.getTrue();

      for (BooleanFormula f : pOperands) {
        long old = ref(result);
        result = JSylvan.makeAnd(result, convert(f));
        deref(old);
      }

      return result;
    }

    @Override
    public Long visitOr(BooleanFormula... pOperands) {
      long result = JSylvan.getFalse();

      for (BooleanFormula f : pOperands) {
        long old = ref(result);
        result = JSylvan.makeOr(result, convert(f));
        deref(old);
      }

      return result;
    }

    @Override
    public Long visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return JSylvan.makeEquals(convert(pOperand1), convert(pOperand2));
    }

    @Override
    protected Long visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
      return JSylvan.makeImplies(convert(pOperand1), convert(pOperand2));
    }

    @Override
    public Long visitIfThenElse(BooleanFormula pCondition,
        BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
      return JSylvan.makeIte(convert(pCondition), convert(pThenFormula), convert(pElseFormula));
    }
  }

  @Override
  public String getVersion() {
    return String.format("Sylvan (%d threads)", threads);
  }
}
