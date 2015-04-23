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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * MutableARGPath is a mutable version of {@link ARGPath}.
 * In contrast to {@link ARGPath}, it does not guarantee non-emptiness,
 * and it is mutable.
 *
 * Consider this class semi-deprecated.
 * Usually it is better to only use immutable {@link ARGPath} objects.
 * When creating ARG paths,
 * it is usually possible to use simple lists of states and edges instead.
 * As a last resort, use {@link ARGPath#mutableCopy()}
 * and {@link MutableARGPath#immutableCopy()} to convert between these two path classes.
 */
public class MutableARGPath extends LinkedList<Pair<ARGState, CFAEdge>> implements Appender {

  private static final long serialVersionUID = -3223480082103314555L;

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on('\n').skipNulls().appendTo(appendable, asEdgesList());
  }

  @Override
  public String toString() {
    return Joiner.on('\n').skipNulls().join(asEdgesList());
  }

  public List<CFAEdge> asEdgesList() {
    return Lists.transform(this, Pair.<CFAEdge>getProjectionToSecond());
  }

  public ImmutableSet<ARGState> getStateSet() {
    List<ARGState> elementList = Lists.transform(this, Pair.<ARGState>getProjectionToFirst());
    return ImmutableSet.copyOf(elementList);
  }

  public ARGPath immutableCopy() {
    return new ARGPath(
        Lists.transform(this, Pair.<ARGState>getProjectionToFirst()),
        Lists.transform(this, Pair.<CFAEdge>getProjectionToSecond()));
  }
}
