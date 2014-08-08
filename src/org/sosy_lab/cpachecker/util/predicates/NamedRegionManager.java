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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders.AbstractAppender;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This class provides a RegionManager which additionally keeps track of a name
 * for each predicate, and can provide a nice String representation of a BDD.
 */
public class NamedRegionManager implements RegionManager {

  private final RegionManager delegate;

  private final BiMap<String, Region> regionMap = HashBiMap.create();

  private static final String ANONYMOUS_PREDICATE = "__anon_pred";
  private int anonymousPredicateCounter = 0;

  /** counter needed for nodes in dot-output */
  int nodeCounter;

  public NamedRegionManager(RegionManager pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  /**
   * Create a predicate with a name associated to it.
   * If the same name is passed again to this method, the old predicate will be
   * returned (guaranteeing uniqueness of predicate<->name mapping).
   * @param pName An arbitary name for a predicate.
   * @return A region representing a predicate
   */
  public Region createPredicate(String pName) {
    Region result = regionMap.get(pName);
    if (result == null) {
      result = delegate.createPredicate();
      regionMap.put(pName, result);
    }
    return result;
  }

  @Override
  public Region createPredicate() {
    return createPredicate(ANONYMOUS_PREDICATE + anonymousPredicateCounter++);
  }

  /**
   * Returns a String representation of a region.
   */
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
        out.append("!")
           .append(predName)
           .append(" & ");
        dumpRegion(falseBranch, out);

      } else if (falseBranch.isFalse()) {
        // only trueBranch is present
        out.append(predName)
           .append(" & ");
        dumpRegion(trueBranch, out);

      } else {
        // both branches present
        out.append("((")
           .append(predName)
           .append(" & ");
        dumpRegion(trueBranch, out);
        out.append(") | (")
           .append("!")
           .append(predName)
           .append(" & ");
        dumpRegion(falseBranch, out);
        out.append("))");
      }
    }
  }

  /** Returns a representation of a region in dot-format (graphviz). */
  public String regionToDot(Region r) {
    nodeCounter = 2; // counter for nodes, values 0 and 1 are used for nodes FALSE and TRUE
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

    regionToDot(r, str, cache);

    str.append("}\n");
    return str.toString();
  }

  private int regionToDot(Region r, StringBuilder str, Map<Region, Integer> cache) {
    if (cache.containsKey(r)) { // use same region again
      return cache.get(r);

    } else {
      Triple<Region, Region, Region> triple = delegate.getIfThenElse(r);

      // create node with label
      String predName = regionMap.inverse().get(triple.getFirst());
      nodeCounter += 1; // one more node is created
      int predNum = nodeCounter;
      str.append(predNum).append(" [label=\"").append(predName).append("\"];\n");

      // create arrow for true branch
      Region trueBranch = triple.getSecond();
      int trueTarget = regionToDot(trueBranch, str, cache);
      str.append(predNum).append(" -> ").append(trueTarget).append(" [style=filled];\n");

      // create arrow for false branch
      Region falseBranch = triple.getThird();
      int falseTarget = regionToDot(falseBranch, str, cache);
      str.append(predNum).append(" -> ").append(falseTarget).append(" [style=dotted];\n");

      cache.put(r, predNum);
      return predNum;
    }
  }

  @Override
  public boolean entails(Region pF1, Region pF2) throws InterruptedException {
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
  public Region fromFormula(BooleanFormula pF, FormulaManagerView pFmgr, Function<BooleanFormula, Region> pAtomToRegion) {
    return delegate.fromFormula(pF, pFmgr, pAtomToRegion);
  }

  @Override
  public Triple<Region, Region, Region> getIfThenElse(Region pF) {
    return delegate.getIfThenElse(pF);
  }

  @Override
  public Set<Region> extractPredicates(Region pF) {
    return delegate.extractPredicates(pF);
  }

  @Override
  public void printStatistics(PrintStream out) {
    out.println("Number of named predicates:          " + (regionMap.size() - anonymousPredicateCounter));
    delegate.printStatistics(out);
  }

  @Override
  public String getVersion() {
    return delegate.getVersion();
  }

  public Set<String> getPredicates() {
    return this.regionMap.keySet();
  }

  @Override
  public Region makeIte(Region pF1, Region pF2, Region pF3) {
    return delegate.makeIte(pF1, pF2, pF3);
  }
}