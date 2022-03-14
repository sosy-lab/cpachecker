// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

class GraphMLTransition {

  private final GraphMLState source;

  private final GraphMLState target;

  private final Optional<String> functionEntry;

  private final Optional<String> functionExit;

  private final Optional<Predicate<FileLocation>> offsetMatcherPredicate;

  private final Optional<Predicate<FileLocation>> lineMatcherPredicate;

  private final AutomatonBoolExpr assumeCaseMatcher;

  private final GraphMLThread thread;

  private final Optional<AutomatonAction> threadAssignment;

  private final Set<String> assumptions;

  private final Optional<String> explicitAssumptionScope;

  private final Optional<String> explicitAssumptionResultFunction;

  private final boolean entersLoopHead;

  public GraphMLTransition(
      GraphMLState pSource,
      GraphMLState pTarget,
      Optional<String> pFunctionEntry,
      Optional<String> pFunctionExit,
      Optional<Predicate<FileLocation>> pOffsetMatcherPredicate,
      Optional<Predicate<FileLocation>> pLineMatcherPredicate,
      AutomatonBoolExpr pAssumeCaseMatcher,
      GraphMLThread pThread,
      Optional<AutomatonAction> pThreadAssignment,
      Set<String> pAssumptions,
      Optional<String> pExplicitAssumptionScope,
      Optional<String> pAssumptionResultFunction,
      boolean pEntersLoopHead) {
    source = Objects.requireNonNull(pSource);
    target = Objects.requireNonNull(pTarget);
    functionEntry = Objects.requireNonNull(pFunctionEntry);
    functionExit = Objects.requireNonNull(pFunctionExit);
    offsetMatcherPredicate = Objects.requireNonNull(pOffsetMatcherPredicate);
    lineMatcherPredicate = Objects.requireNonNull(pLineMatcherPredicate);
    assumeCaseMatcher = Objects.requireNonNull(pAssumeCaseMatcher);
    thread = Objects.requireNonNull(pThread);
    threadAssignment = Objects.requireNonNull(pThreadAssignment);
    assumptions = ImmutableSet.copyOf(pAssumptions);
    explicitAssumptionScope = Objects.requireNonNull(pExplicitAssumptionScope);
    explicitAssumptionResultFunction = Objects.requireNonNull(pAssumptionResultFunction);
    entersLoopHead = pEntersLoopHead;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof GraphMLTransition) {
      GraphMLTransition other = (GraphMLTransition) pOther;
      return getSource().equals(other.getSource())
          && getTarget().equals(other.getTarget())
          && getFunctionEntry().equals(other.getFunctionEntry())
          && getFunctionExit().equals(other.getFunctionExit())
          && getOffsetMatcherPredicate().equals(other.getOffsetMatcherPredicate())
          && getLineMatcherPredicate().equals(other.getLineMatcherPredicate())
          && getAssumeCaseMatcher().equals(other.getAssumeCaseMatcher())
          && getThread().equals(other.getThread())
          && getThreadAssignment().equals(other.getThreadAssignment())
          && getAssumptions().equals(other.getAssumptions())
          && getExplicitAssumptionScope().equals(other.getExplicitAssumptionScope())
          && getExplicitAssumptionResultFunction()
              .equals(other.getExplicitAssumptionResultFunction())
          && entersLoopHead() == other.entersLoopHead();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSource(),
        getTarget(),
        getFunctionEntry(),
        getFunctionExit(),
        getOffsetMatcherPredicate(),
        getLineMatcherPredicate(),
        getAssumeCaseMatcher(),
        getThread(),
        getThreadAssignment(),
        getAssumptions(),
        getExplicitAssumptionScope(),
        getExplicitAssumptionResultFunction(),
        entersLoopHead());
  }

  @Override
  public String toString() {
    return String.format("(%s -> %s)", getSource(), getTarget());
  }

  public GraphMLState getSource() {
    return source;
  }

  public GraphMLState getTarget() {
    return target;
  }

  public Optional<String> getFunctionEntry() {
    return functionEntry;
  }

  public Optional<String> getFunctionExit() {
    return functionExit;
  }

  public Optional<Predicate<FileLocation>> getOffsetMatcherPredicate() {
    return offsetMatcherPredicate;
  }

  public Optional<Predicate<FileLocation>> getLineMatcherPredicate() {
    return lineMatcherPredicate;
  }

  public AutomatonBoolExpr getAssumeCaseMatcher() {
    return assumeCaseMatcher;
  }

  public GraphMLThread getThread() {
    return thread;
  }

  public Optional<AutomatonAction> getThreadAssignment() {
    return threadAssignment;
  }

  public Set<String> getAssumptions() {
    return assumptions;
  }

  public Optional<String> getExplicitAssumptionScope() {
    return explicitAssumptionScope;
  }

  public Optional<String> getExplicitAssumptionResultFunction() {
    return explicitAssumptionResultFunction;
  }

  public boolean entersLoopHead() {
    return entersLoopHead;
  }

  public static GraphMLThread createThread(int pId, String pName) {
    return new GraphMLThread(pId, pName);
  }

  static class GraphMLThread implements Comparable<GraphMLThread> {

    private final int id;

    private final String name;

    private GraphMLThread(int pId, String pName) {
      id = pId;
      name = Objects.requireNonNull(pName);
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof GraphMLThread) {
        GraphMLThread other = (GraphMLThread) pOther;
        return id == other.id && name.equals(other.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return id;
    }

    public int getId() {
      return id;
    }

    @Override
    public String toString() {
      if (name.equals(Integer.toString(id))) {
        return name;
      }
      return String.format("%s (%d)", name, id);
    }

    @Override
    public int compareTo(GraphMLThread pOther) {
      return ComparisonChain.start().compare(id, pOther.id).compare(name, pOther.name).result();
    }
  }
}
