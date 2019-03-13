/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter.TargetTestVector;

public class TestAppender {

  private final Appendable appendable;

  public TestAppender(Appendable pTarget) {
    appendable = pTarget;
  }


  public TestAppender appendln() throws IOException {
    appendable.append(System.lineSeparator());
    return this;
  }

  public TestAppender append(CharSequence pCsq) {
    try {
      appendable.append(pCsq);
    } catch (IOException e) {
      throw new AssertionError();
    }
    return this;
  }


  public void appendTest(Optional<TargetTestVector> pTestVector) {
    TargetTestVector targetTestVector = pTestVector.get();
    TestVector testVector = targetTestVector.getTestVector();
    PersistentSortedMap<ComparableFunctionDeclaration, ImmutableList<Integer>> functionsWithIndices =
        testVector.getPointerIndices();

    Map<ComparableFunctionDeclaration, ImmutableList<Integer>> mutableMap =
        new HashMap<>(functionsWithIndices);
    Iterator<Map.Entry<ComparableFunctionDeclaration, ImmutableList<Integer>>> mapIterator =
        mutableMap.entrySet().iterator();
    while (mapIterator.hasNext()) {
      Map.Entry<ComparableFunctionDeclaration, ImmutableList<Integer>> entry = mapIterator.next();
      ComparableFunctionDeclaration key = entry.getKey();
      String name = key.getDeclaration().getName();
      append(name);
      append(",");
      Iterator<Integer> listIterator = entry.getValue().iterator();
      while (listIterator.hasNext()) {
        int value = listIterator.next();
        append(Integer.toString(value));
        if (listIterator.hasNext()) {
          append(",");
        }
      }
      try {
        appendln();
      } catch (IOException e) {
        throw new AssertionError();
      }

    }
    try {
      appendln();
    } catch (IOException e) {
      throw new AssertionError();
    }

  }

}
