/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import java.util.Set;

import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.PartitioningOperator;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


public class DefaultPartitioningOperator implements PartitioningOperator {

  /**
   *
   * @return  Either the bisect partitioning,
   *          or the same partitioning as before
   *            (if all partitions consist already of one element)
   */
  @VisibleForTesting
  ImmutableSet<ImmutableSet<Property>> bisectPartitons(ImmutableSet<ImmutableSet<Property>> pInput) {
    ImmutableSet.Builder<ImmutableSet<Property>> result = ImmutableSet.<ImmutableSet<Property>>builder();

    for (final ImmutableSet<Property> p: pInput) {
      final ImmutableList<Property> l = p.asList();

      if (l.size() > 1) {

        int firstElementsToAdd = (int) Math.ceil(l.size() / 2.0);

        result.add(ImmutableSet.<Property>copyOf(l.subList(0, firstElementsToAdd)));
        result.add(ImmutableSet.<Property>copyOf(l.subList(firstElementsToAdd, l.size())));

      } else {
        result.add(p);
      }
    }

    return result.build();
  }

  @Override
  public ImmutableSet<ImmutableSet<Property>> partition(
      ImmutableSet<ImmutableSet<Property>> pLastCheckedPartitioning,
      Set<Property> pToCheck) {

    ImmutableSet<ImmutableSet<Property>> result = ImmutableSet.of(ImmutableSet.copyOf(pToCheck));

    // At least as much partitions as in pLastCheckedPartitioning
    if (result.size() < pLastCheckedPartitioning.size()) {
      result = bisectPartitons(result);
    }

    // If possible not equal to the last partitioning
    if (pLastCheckedPartitioning.equals(result)) {
      return bisectPartitons(result);
    }

    return result;
  }

}
