// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition.Scope;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

/**
 * An edge corresponds to the transfer from one node to another. This class is intended to be
 * immutable.
 */
public class Edge implements Comparable<Edge> {

  private final String source;

  private final String target;

  private final TransitionCondition label;

  private final int hashCode;

  public Edge(String pSource, String pTarget, TransitionCondition pLabel) {
    Preconditions.checkNotNull(pSource);
    Preconditions.checkNotNull(pTarget);
    Preconditions.checkNotNull(pLabel);
    source = pSource;
    target = pTarget;
    label = pLabel;

    // we assume immutable members and can eagerly compute the hashcode
    hashCode = Objects.hash(label, source, target);
  }

  @Override
  public String toString() {
    return String.format("{%s -- %s --> %s}", source, label, target);
  }

  @Override
  public int compareTo(Edge pO) {
    return ComparisonChain.start()
        .compare(source, pO.source)
        .compare(target, pO.target)
        .compare(label, pO.label)
        .result();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Edge) {
      Edge other = (Edge) pOther;
      return hashCode == other.hashCode
          && source.equals(other.source)
          && target.equals(other.target)
          && label.equals(other.label);
    }
    return false;
  }

  public String getSource() {
    return source;
  }

  public String getTarget() {
    return target;
  }

  public TransitionCondition getLabel() {
    return label;
  }

  /**
   * This method tries to merge the current edge with another edge.
   *
   * @return the merged edge or nothing.
   */
  public Optional<Edge> tryMerge(Edge pOther) {
    // only merge edges from matching source and targets.
    if (!source.equals(pOther.source) || !target.equals(pOther.target)) {
      return Optional.empty();
    }
    MapDifference<KeyDef, String> difference =
        Maps.difference(label.getMapping(), pOther.label.getMapping());
    // only merge edges if label mappings are comparable.
    if (!difference.entriesOnlyOnLeft().isEmpty() || !difference.entriesOnlyOnRight().isEmpty()) {
      return Optional.empty();
    }
    TransitionCondition newLabel = pOther.label;
    Optional<Scope> newScope = label.getScope().mergeWith(newLabel.getScope());
    if (!newScope.isPresent()) {
      return Optional.empty();
    }
    newLabel = newLabel.withScope(newScope.orElseThrow());
    newLabel = newLabel.putAllAndCopy(label);
    // merge information from the label mappings.
    for (Map.Entry<KeyDef, ValueDifference<String>> diffEntry :
        difference.entriesDiffering().entrySet()) {
      KeyDef key = diffEntry.getKey();
      ValueDifference<String> diff = diffEntry.getValue();
      final String result;
      switch (key) {
        case STARTLINE:
        case OFFSET:
          int lowA = Integer.parseInt(diff.leftValue());
          int lowB = Integer.parseInt(diff.rightValue());
          result = Integer.toString(Math.min(lowA, lowB));
          break;
        case ENDLINE:
        case ENDOFFSET:
          int highA = Integer.parseInt(diff.leftValue());
          int highB = Integer.parseInt(diff.rightValue());
          result = Integer.toString(Math.max(highA, highB));
          break;
        default:
          // incomparable information
          return Optional.empty();
      }
      newLabel = newLabel.putAndCopy(key, result);
    }
    return Optional.of(new Edge(source, target, newLabel));
  }
}
