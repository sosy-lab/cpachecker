// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

record GraphMLTransition(
    GraphMLState source,
    GraphMLState target,
    Optional<String> functionEntry,
    Optional<String> functionExit,
    Optional<Predicate<FileLocation>> offsetMatcherPredicate,
    Optional<Predicate<FileLocation>> lineMatcherPredicate,
    AutomatonBoolExpr assumeCaseMatcher,
    GraphMLThread thread,
    Optional<AutomatonAction> threadAssignment,
    Set<String> assumptions,
    Optional<String> explicitAssumptionScope,
    Optional<String> explicitAssumptionResultFunction,
    boolean entersLoopHead) {

  GraphMLTransition {
    Objects.requireNonNull(source);
    Objects.requireNonNull(target);
    Objects.requireNonNull(functionEntry);
    Objects.requireNonNull(functionExit);
    Objects.requireNonNull(offsetMatcherPredicate);
    Objects.requireNonNull(lineMatcherPredicate);
    Objects.requireNonNull(assumeCaseMatcher);
    Objects.requireNonNull(thread);
    Objects.requireNonNull(threadAssignment);
    Objects.requireNonNull(assumptions);
    Objects.requireNonNull(explicitAssumptionScope);
    Objects.requireNonNull(explicitAssumptionResultFunction);
  }

  @Override
  public String toString() {
    return String.format("(%s -> %s)", source(), target());
  }

  static GraphMLThread createThread(int pId, String pName) {
    return new GraphMLThread(pId, pName);
  }

  record GraphMLThread(int id, String name) implements Comparable<GraphMLThread> {

    GraphMLThread {
      Objects.requireNonNull(name);
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
