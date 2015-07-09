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
package org.sosy_lab.cpachecker.util.predicates.princess;

import static scala.collection.JavaConversions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;

import scala.Enumeration.Value;
import scala.Option;
import scala.collection.Seq;
import scala.collection.mutable.ArrayBuffer;
import ap.SimpleAPI;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFunction;
import ap.parser.ITerm;

import com.google.common.collect.ImmutableList;

/** This is a Wrapper around some parts of the PrincessAPI.
 * It allows to have a stack with operations like: push, pop, assert, checkSat, getInterpolants, getModel.
 * A stack is always connected with a PrincessEnvironment, because Variables are declared there.
 * One PrincessEnvironment can manage several stacks.
 *
 * This implementation also tracks symbols (boolean or integer variables, uninterpreted functions),
 * because in SMTLIB symbols would be deleted after a pop-operation.
 * We track symbols in our own stack and recreate them after the pop-operation. */
class SymbolTrackingPrincessStack implements PrincessStack {

  /** the wrapped api */
  private final PrincessEnvironment env;
  private final SimpleAPI api;
  private final boolean usableForInterpolation;
  private final ShutdownNotifier shutdownNotifier;

  /** data-structures for tracking symbols */
  private final Deque<Level> trackingStack = new ArrayDeque<>();

  public SymbolTrackingPrincessStack(final PrincessEnvironment env, final SimpleAPI api, boolean usableForInterpolation, ShutdownNotifier shutdownNotifier) {
    this.env = env;
    this.api = api;
    this.usableForInterpolation = usableForInterpolation;
    this.shutdownNotifier = shutdownNotifier;
  }

  public boolean canBeUsedForInterpolation() {
    return usableForInterpolation;
  }

  @Override
  public PrincessEnvironment getEnv() {
    return env;
  }

  @Override
  public void push(int levels) {
    for (int i = 0; i < levels; i++) {
      api.push();
      trackingStack.addLast(new Level());
    }
  }

  /** This function pops levels from the assertion-stack. */
  @Override
  public void pop(int levels) {
    // we have to recreate symbols on lower levels, because CPAchecker assumes "global" symbols.
    final Deque<Level> toAdd = new ArrayDeque<>(levels);
    for (int i = 0; i < levels; i++) {
      api.pop();
      toAdd.add(trackingStack.removeLast());
    }
    for (Level level : toAdd) {
      api.addBooleanVariables(iterableAsScalaIterable(level.booleanSymbols));
      api.addConstants(iterableAsScalaIterable(level.intSymbols));
      for (IFunction function : level.functionSymbols) {
        api.addFunction(function);
      }
      for (Pair<IExpression, IExpression> abbrev : level.abbreviations) {
        addAbbrevToStack(abbrev.getFirst(), abbrev.getSecond());
      }
      if (!trackingStack.isEmpty()) {
        trackingStack.getLast().mergeWithHigher(level);
      }
    }
  }

  /** This function adds the term on top of the stack. */
  @Override
  public void assertTerm(IFormula booleanFormula) {
    api.addAssertion(booleanFormula);
  }

  /** This function sets a partition number for all the term,
   *  that are asserted  after calling this method, until a new partition number is set. */
  @Override
  public void assertTermInPartition(IFormula booleanFormula, int index) {
    // set partition number and add formula
    api.setPartitionNumber(index);
    api.addAssertion(booleanFormula);

    // reset partition number to magic number -1, that represents formulae belonging to all partitions.
    api.setPartitionNumber(-1);
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  @Override
  public boolean checkSat() {
    final Value result = api.checkSat(true);
    if (result == SimpleAPI.ProverStatus$.MODULE$.Sat()) {
      return true;
    } else if (result == SimpleAPI.ProverStatus$.MODULE$.Unsat()) {
      return false;
    } else {
      throw new AssertionError("checkSat returned " + result);
    }
  }

  @Override
  public SimpleAPI.PartialModel getPartialModel() {
    return api.partialModel();
  }

  @Override
  public Option<Object> evalPartial(IFormula formula) {
    return api.evalPartial(formula);
  }

  /** This function returns a list of interpolants for the partitions.
   * Each partition contains the indizes of its terms.
   * There will be (n-1) interpolants for n partitions. */
  @Override
  public List<IFormula> getInterpolants(List<Set<Integer>> partitions) {

    // convert to needed data-structure
    final ArrayBuffer<scala.collection.immutable.Set<Object>> args = new ArrayBuffer<>();
    for (Set<Integer> partition : partitions) {
      args.$plus$eq(asScalaSet(partition).toSet());
    }

    // do the hard work
    final Seq<IFormula> itps = api.getInterpolants(args.toSeq(), api.getInterpolants$default$2());

    assert itps.length() == partitions.size() - 1 : "There should be (n-1) interpolants for n partitions";

    // convert data-structure back
    return ImmutableList.copyOf(asJavaCollection(itps));
  }

  @Override
  public void close() {
    // if a timeout is reached we do not want to do possibly long lasting
    // pop operations (with copying variables to lower tiers of the stack)
    if (shutdownNotifier.shouldShutdown()) {
      env.removeStack(this);
      api.shutDown();
    } else {
      pop(trackingStack.size());
      env.unregisterStack(this);
    }
  }

  /** add external definition: boolean variable. */
  void addSymbol(IFormula f) {
    api.addBooleanVariable(f);
    if (!trackingStack.isEmpty()) {
      trackingStack.getLast().booleanSymbols.add(f);
    }
  }

  /** add external definition: integer variable. */
  void addSymbol(ITerm f) {
    api.addConstant(f);
    if (!trackingStack.isEmpty()) {
      trackingStack.getLast().intSymbols.add(f);
    }
  }

  /** add external definition: uninterpreted function. */
  void addSymbol(IFunction f) {
    api.addFunction(f);
    if (!trackingStack.isEmpty()) {
      trackingStack.getLast().functionSymbols.add(f);
    }
  }

  /** add external definition: abbreviation for formula. */
  void addAbbrev(IExpression abbrev, IExpression formula) {
    addAbbrevToStack(abbrev, formula);
    if (!trackingStack.isEmpty()) {
      trackingStack.getLast().abbreviations.add(Pair.of(abbrev, formula));
    }
  }

  private void addAbbrevToStack(IExpression abbrev, IExpression formula) {
    if (abbrev instanceof IFormula) {
      api.addAbbrev((IFormula)abbrev, (IFormula)formula);
    } else if (abbrev instanceof ITerm) {
      api.addAbbrev((ITerm)abbrev, (ITerm)formula);
    } else {
      throw new AssertionError("No abbreviation possible for " + abbrev.getClass());
    }
  }

  private static class Level {
    List<IFormula> booleanSymbols = new ArrayList<>();
    List<ITerm> intSymbols = new ArrayList<>();
    List<IFunction> functionSymbols = new ArrayList<>();

    // order is important for abbreviations, because a abbreviation might depend on another one.
    List<Pair<IExpression, IExpression>> abbreviations = new ArrayList<>();

    /**  add higher level to current level, we keep the order of creating symbols. */
    void mergeWithHigher(Level other) {
      this.booleanSymbols.addAll(other.booleanSymbols);
      this.intSymbols.addAll(other.intSymbols);
      this.functionSymbols.addAll(other.functionSymbols);
      this.abbreviations.addAll(other.abbreviations);
    }
  }
}
