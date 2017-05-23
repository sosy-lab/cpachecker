/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.io.IOException;
import java.util.Map;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

/**
 * This class represents a model returned from an SMT solver:
 * assignments from terms to their values.
 *
 * If you are looking for rich meta-data used in the
 * {@link org.sosy_lab.cpachecker.core.CounterexampleInfo},
 * you might be after
 * {@link org.sosy_lab.cpachecker.core.counterexample.RichModel}.
 */
public class Model extends ForwardingMap<AssignableTerm, Object> implements Appender {
  private final ImmutableMap<AssignableTerm, Object> data;

  public Model(Map<AssignableTerm, Object> pData) {
    data = ImmutableMap.copyOf(pData);
  }

  public ImmutableMap<AssignableTerm, Object> getData() {
    return data;
  }

  public static Model empty() {
    return new Model(ImmutableMap.<AssignableTerm, Object>of());
  }

  @Override
  protected Map<AssignableTerm, Object> delegate() {
    return data;
  }

  private static final MapJoiner joiner = Joiner.on(System.lineSeparator()).withKeyValueSeparator(": ");

  public static void appendModel(Appendable output, Map<AssignableTerm, Object> data)
      throws IOException {
    Map<AssignableTerm, Object> sorted = ImmutableSortedMap.copyOf(data,
        Ordering.usingToString());
    joiner.appendTo(output, sorted);
  }

  @Override
  public void appendTo(Appendable output) throws IOException {
    appendModel(output, data);
  }

  @Override
  public String toString() {
    return Appenders.toString(this);
  }
}
