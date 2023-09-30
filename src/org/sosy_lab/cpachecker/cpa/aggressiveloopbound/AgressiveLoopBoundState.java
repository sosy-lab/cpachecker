// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.aggressiveloopbound;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class AgressiveLoopBoundState implements AbstractState, Targetable, Graphable {

  private final boolean stopIt;

  private final Map<Loop, Integer> loopHeadsVisited;

  private final ArrayDeque<Loop> loopStack;

  private boolean anyLoopSeen;

  public AgressiveLoopBoundState() {
    this(new HashMap<>(), false);
  }

  AgressiveLoopBoundState(
      Map<Loop, Integer> pLoopHeadsVisited,
      boolean pStopIt,
      ArrayDeque<Loop> pStack,
      boolean pAnyLoopSeen) {
    this.loopHeadsVisited = pLoopHeadsVisited;
    this.stopIt = pStopIt;
    this.loopStack = pStack;
    this.anyLoopSeen = pAnyLoopSeen;
  }

  AgressiveLoopBoundState(Map<Loop, Integer> pLoopHeadsVisited, boolean pStopIt) {
    this.loopHeadsVisited = pLoopHeadsVisited;
    this.stopIt = pStopIt;
    this.loopStack = new ArrayDeque<>();
    this.anyLoopSeen = false;
  }

  public AgressiveLoopBoundState copy() {
    return new AgressiveLoopBoundState(
        new HashMap<>(this.loopHeadsVisited),
        this.stopIt,
        new ArrayDeque<>(this.loopStack),
        this.anyLoopSeen);
  }

  public AgressiveLoopBoundState exit(Loop pOldLoop) {
    // if the stack contains the loopbound, remove all elements that are before this element
    if (this.loopStack.contains(pOldLoop)) {
      Loop removedElem;
      do {
        removedElem = loopStack.removeFirst();
        this.loopHeadsVisited.put(removedElem, 0);
      } while (!removedElem.equals(pOldLoop));
    }
    return copy();
  }

  public AgressiveLoopBoundState enter(Loop pLoop) {
    if (this.loopHeadsVisited.containsKey(pLoop)) {
      this.loopHeadsVisited.put(pLoop, this.loopHeadsVisited.get(pLoop) + 1);

    } else {
      this.loopHeadsVisited.put(pLoop, 1);
    }
    this.anyLoopSeen = true;
    return copy();
  }

  public AgressiveLoopBoundState visitLoopHeadInitially(Loop pLoop) {
    this.loopHeadsVisited.put(pLoop, 0);
    this.anyLoopSeen = true;
    return new AgressiveLoopBoundState(new HashMap<>(this.loopHeadsVisited), this.stopIt);
  }

  public AgressiveLoopBoundState setStop(boolean pStop) {
    if (stopIt == pStop) {
      return this;
    }
    return new AgressiveLoopBoundState(this.loopHeadsVisited, pStop);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof AgressiveLoopBoundState that)) {
      return false;
    }
    return stopIt == that.stopIt
        && loopHeadsVisited.equals(that.loopHeadsVisited)
        && anyLoopSeen == that.anyLoopSeen;
  }

  @Override
  public int hashCode() {
    int hashCache = 0;
    return Objects.hash(stopIt, hashCache, loopHeadsVisited, anyLoopSeen);
  }

  @Override
  public boolean isTarget() {
    return false;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return new HashSet<>();
  }

  @Override
  public String toString() {
    return this.loopHeadsVisited.entrySet().stream()
        .map(
            e ->
                String.format("Loop: %s -> %s", e.getKey().getLoopHeads().toString(), e.getValue()))
        .collect(Collectors.joining());
  }

  public int getVisits(Loop pOldLoop) {
    return this.loopHeadsVisited.getOrDefault(pOldLoop, -1);
  }

  public boolean isStop() {
    return this.stopIt;
  }

  public boolean isAnyLoopSeen() {
    return anyLoopSeen;
  }

  @Override
  public String toDOTLabel() {
    return this.loopHeadsVisited.entrySet().stream()
            .map(
                e ->
                    String.format(
                        "Loop: %s -> %s", e.getKey().getLoopHeads().toString(), e.getValue()))
            .collect(Collectors.joining())
        + System.lineSeparator()
        + "Stack:"
        + this.loopStack.stream()
            .map(e -> e.getLoopHeads().toString())
            .collect(Collectors.joining())
        + System.lineSeparator()
        + (anyLoopSeen ? "LoopsSeen" : "NoLoopsSeen");
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public AgressiveLoopBoundState addToStack(Loop pEnteredLoop) {
    this.loopStack.addFirst(pEnteredLoop);
    this.loopHeadsVisited.putIfAbsent(pEnteredLoop, 0);
    this.anyLoopSeen = true;
    return this.copy();
  }

  public ArrayDeque<Loop> getLoopStack() {
    return loopStack;
  }
}
