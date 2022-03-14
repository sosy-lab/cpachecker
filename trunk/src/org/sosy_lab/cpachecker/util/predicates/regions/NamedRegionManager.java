// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.regions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.ImmutableIntArray;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class provides a RegionManager which additionally keeps track of a name for each predicate,
 * and can provide a nice String representation of a BDD.
 *
 * <p>This class is thread-safe, iff the delegated {@link RegionManager} is thread-safe.
 */
public class NamedRegionManager implements RegionManager {

  private static final String ANONYMOUS_PREDICATE = "__anon_pred";
  private final RegionManager delegate;
  private final BiMap<String, Region> regionMap = Maps.synchronizedBiMap(HashBiMap.create());
  private AtomicInteger anonymousPredicateCounter = new AtomicInteger(0);

  public NamedRegionManager(RegionManager pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  /**
   * Create a predicate with a name associated to it. If the same name is passed again to this
   * method, the old predicate will be returned (guaranteeing uniqueness of predicate<->name
   * mapping).
   *
   * @param pName An arbitary name for a predicate.
   * @return A region representing a predicate
   */
  public Region createPredicate(String pName) {
    return regionMap.computeIfAbsent(pName, ignoreArg -> delegate.createPredicate());
  }

  @Override
  public Region createPredicate() {
    return createPredicate(ANONYMOUS_PREDICATE + anonymousPredicateCounter.getAndIncrement());
  }

  /** Returns a String representation of a region. */
  public Appender dumpRegion(final Region r) {
    return new AbstractAppender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        dumpRegion(r, pAppendable);
      }
    };
  }

  private void dumpRegion(Region r, Appendable out) throws IOException {
    if (regionMap.containsValue(r)) {
      out.append(regionMap.inverse().get(r));

    } else if (r.isFalse()) {
      out.append("FALSE");

    } else if (r.isTrue()) {
      out.append("TRUE");

    } else {
      Triple<Region, Region, Region> triple = delegate.getIfThenElse(r);
      String predName = regionMap.inverse().get(triple.getFirst());

      Region trueBranch = triple.getSecond();
      Region falseBranch = triple.getThird();

      if (trueBranch.isFalse()) {
        assert !falseBranch.isFalse();
        // only falseBranch is present
        out.append("!").append(predName).append(" & ");
        dumpRegion(falseBranch, out);

      } else if (falseBranch.isFalse()) {
        // only trueBranch is present
        out.append(predName).append(" & ");
        dumpRegion(trueBranch, out);

      } else {
        // both branches present
        out.append("((").append(predName).append(" & ");
        dumpRegion(trueBranch, out);
        out.append(") | (").append("!").append(predName).append(" & ");
        dumpRegion(falseBranch, out);
        out.append("))");
      }
    }
  }

  /** Returns a representation of a region in dot-format (graphviz). */
  public String regionToDot(Region r) {
    // counter for nodes, values 0 and 1 are used for nodes FALSE and TRUE.
    // we use a reference to an integer to be able to change its value in called methods.
    AtomicInteger nodeCounter = new AtomicInteger(2);
    Map<Region, Integer> cache = new HashMap<>(); // map for same regions
    StringBuilder str = new StringBuilder("digraph G {\n");

    // make nodes for FALSE and TRUE
    if (!r.isTrue()) {
      str.append("0 [shape=box, label=\"0\", style=filled, shape=box, height=0.3, width=0.3];\n");
      cache.put(makeFalse(), 0);
    }
    if (!r.isFalse()) {
      str.append("1 [shape=box, label=\"1\", style=filled, shape=box, height=0.3, width=0.3];\n");
      cache.put(makeTrue(), 1);
    }

    regionToDot(r, str, cache, nodeCounter);

    str.append("}\n");
    return str.toString();
  }

  /** Appends a sub-tree to the String and increments the nodeCounter. */
  private int regionToDot(
      Region r, StringBuilder str, Map<Region, Integer> cache, AtomicInteger nodeCounter) {
    if (cache.containsKey(r)) { // use same region again
      return cache.get(r);

    } else {
      Triple<Region, Region, Region> triple = delegate.getIfThenElse(r);

      // create node with label
      String predName = regionMap.inverse().get(triple.getFirst());
      int predNum = nodeCounter.incrementAndGet(); // one more node is created
      str.append(predNum).append(" [label=\"").append(predName).append("\"];\n");

      // create arrow for true branch
      Region trueBranch = triple.getSecond();
      int trueTarget = regionToDot(trueBranch, str, cache, nodeCounter);
      str.append(predNum).append(" -> ").append(trueTarget).append(" [style=filled];\n");

      // create arrow for false branch
      Region falseBranch = triple.getThird();
      int falseTarget = regionToDot(falseBranch, str, cache, nodeCounter);
      str.append(predNum).append(" -> ").append(falseTarget).append(" [style=dotted];\n");

      cache.put(r, predNum);
      return predNum;
    }
  }

  @Override
  public boolean entails(Region pF1, Region pF2) throws SolverException, InterruptedException {
    return delegate.entails(pF1, pF2);
  }

  @Override
  public Region makeTrue() {
    return delegate.makeTrue();
  }

  @Override
  public Region makeFalse() {
    return delegate.makeFalse();
  }

  @Override
  public Region makeNot(Region pF) {
    return delegate.makeNot(pF);
  }

  @Override
  public Region makeAnd(Region pF1, Region pF2) {
    return delegate.makeAnd(pF1, pF2);
  }

  @Override
  public Region makeOr(Region pF1, Region pF2) {
    return delegate.makeOr(pF1, pF2);
  }

  @Override
  public Region makeEqual(Region pF1, Region pF2) {
    return delegate.makeEqual(pF1, pF2);
  }

  @Override
  public Region makeUnequal(Region pF1, Region pF2) {
    return delegate.makeUnequal(pF1, pF2);
  }

  @Override
  public Region makeExists(Region pF1, Region... pF2) {
    return delegate.makeExists(pF1, pF2);
  }

  @Override
  public RegionBuilder builder(ShutdownNotifier pShutdownNotifier) {
    return delegate.builder(pShutdownNotifier);
  }

  @Override
  public void setVarOrder(ImmutableIntArray pOrder) {
    delegate.setVarOrder(pOrder);
  }

  @Override
  public void reorder(VariableOrderingStrategy strategy) {
    delegate.reorder(strategy);
  }

  @Override
  public Region fromFormula(
      BooleanFormula pF, FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
    return delegate.fromFormula(pF, pFmgr, pAtomToRegion);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    return delegate.getIfThenElse(pF);
  }

  @Override
  public void printStatistics(PrintStream out) {
    out.println(
        "Number of named predicates:          "
            + (regionMap.size() - anonymousPredicateCounter.get()));
    delegate.printStatistics(out);
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  public Set<String> getPredicates() {
    synchronized (regionMap) {
      return ImmutableSet.copyOf(regionMap.keySet());
    }
  }

  /**
   * Get a snapshot of the current variable ordering in the whole BDD library, limited to all named
   * regions managed by this manager. .
   *
   * <p>This method also works if the delegated manager changed its ordering internally, as long as
   * the ordering is not changed while running this method.
   */
  public List<String> getOrderedPredicates() {
    synchronized (regionMap) {
      // sort predicates according to BDD ordering.
      // create small BDDs "AND(A,B)" and check which node is the root.
      return ImmutableList.sortedCopyOf(
          (a, b) -> {
            Region ra = regionMap.get(a);
            Region rb = regionMap.get(b);
            Region root = getIfThenElse(makeAnd(ra, rb)).getFirst();
            if (ra.equals(root)) {
              return 1;
            } else if (rb.equals(root)) {
              return -1;
            } else {
              throw new AssertionError("should not happen, all predicates are unique");
            }
          },
          regionMap.keySet());
    }
  }

  /** Get the current variables in the BDD. */
  public Set<String> getPredicatesFromRegion(Region region) {
    synchronized (regionMap) {
      Set<String> predicates = new LinkedHashSet<>();
      Set<Region> finished = new HashSet<>();
      Deque<Region> waitlist = new ArrayDeque<>();
      waitlist.push(region);
      while (!waitlist.isEmpty()) {
        Region r = waitlist.pop();
        if (r.isTrue() || r.isFalse() || !finished.add(r)) {
          continue;
        }
        Triple<Region, Region, Region> t = getIfThenElse(r);
        predicates.add(regionMap.inverse().get(t.getFirst()));
        waitlist.add(t.getSecond());
        waitlist.add(t.getThird());
      }
      return predicates;
    }
  }

  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    return delegate.makeIte(pF1, pF2, pF3);
  }

  @Override
  public Region replace(Region pRegion, List<Region> pOldPredicates, List<Region> pNewPredicates) {
    return delegate.replace(pRegion, pOldPredicates, pNewPredicates);
  }
}
