/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

enum InvariantsDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public InvariantsElement join(AbstractElement pElement1, AbstractElement pElement2) {
    InvariantsElement element1 = (InvariantsElement)pElement1;
    InvariantsElement element2 = (InvariantsElement)pElement2;

    MapDifference<String, SimpleInterval> differences = Maps.difference(element1.getIntervals(), element2.getIntervals());

    if (differences.areEqual()) {
      return element2;
    }

    Map<String, SimpleInterval> result = new HashMap<String, SimpleInterval>(element1.getIntervals().size());
    result.putAll(differences.entriesInCommon());

    for (Entry<String, ValueDifference<SimpleInterval>> entry : differences.entriesDiffering().entrySet()) {
      ValueDifference<SimpleInterval> values = entry.getValue();
      result.put(entry.getKey(), SimpleInterval.span(values.leftValue(), values.rightValue()));
    }

    return new InvariantsElement(result);
  }

  @Override
  public boolean isLessOrEqual(AbstractElement pElement1, AbstractElement pElement2) {
    // check whether element 1 (or left) contains more information than element 2 (or right)
    InvariantsElement element1 = (InvariantsElement)pElement1;
    InvariantsElement element2 = (InvariantsElement)pElement2;

    MapDifference<String, SimpleInterval> differences = Maps.difference(element1.getIntervals(), element2.getIntervals());

    if (differences.areEqual()) {
      return true;
    }

    if (!differences.entriesOnlyOnRight().isEmpty()) {
      // right knows more, so it is not greater than left
      return false;
    }

    for (ValueDifference<SimpleInterval> values : differences.entriesDiffering().values()) {
      if (!values.rightValue().contains(values.leftValue())) {
        // right is more specific, so it has more information
        return false;
      }
    }

    return true;
  }

}
