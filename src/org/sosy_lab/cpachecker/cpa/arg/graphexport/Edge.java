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
package org.sosy_lab.cpachecker.cpa.arg.graphexport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import java.util.Objects;

/**
 * An edge corresponds to the transfer from one node to another.
 * This class is intended to be immutable.
 */
public class Edge implements Comparable<Edge> {

  public final String source;

  public final String target;

  public final TransitionCondition label;

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
    return Objects.hash(source, target, label);
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
}