/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

/**
 * An edge corresponds to the transfer from one node to another.
 * This class is intended to be immutable.
 */
class Edge implements Comparable<Edge> {

  private final String source;

  private final String target;

  private final TransitionCondition label;

  private int hashCode = 0;

  public Edge(String pSource, String pTarget, TransitionCondition pLabel) {
    Preconditions.checkNotNull(pSource);
    Preconditions.checkNotNull(pTarget);
    Preconditions.checkNotNull(pLabel);
    this.source = pSource;
    this.target = pTarget;
    this.label = pLabel;
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
    if (hashCode == 0) {
      final int prime = 31;
      hashCode = prime + ((label == null) ? 0 : label.hashCode());
      hashCode = prime * hashCode + ((source == null) ? 0 : source.hashCode());
      hashCode = prime * hashCode + ((target == null) ? 0 : target.hashCode());
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof Edge) {
      Edge other = (Edge) pOther;
      return source.equals(other.source)
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

  public Optional<Edge> tryMerge(Edge pOther) {
    if (!source.equals(pOther.source)) {
      return Optional.empty();
    }
    if (!target.equals(pOther.target)) {
      return Optional.empty();
    }
    MapDifference<KeyDef, String> difference =
        Maps.difference(label.getMapping(), pOther.label.getMapping());
    if (!difference.entriesOnlyOnLeft().isEmpty() || !difference.entriesOnlyOnRight().isEmpty()) {
      return Optional.empty();
    }
    TransitionCondition newLabel = pOther.label;
    newLabel = newLabel.putAllAndCopy(label);
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
          return Optional.empty();
      }
      newLabel = newLabel.putAndCopy(key, result);
    }
    return Optional.of(new Edge(source, target, newLabel));
  }
}
