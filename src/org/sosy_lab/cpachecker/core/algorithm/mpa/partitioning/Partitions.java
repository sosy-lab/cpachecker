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
package org.sosy_lab.cpachecker.core.algorithm.mpa.partitioning;

import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.core.algorithm.mpa.interfaces.Partitioning;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class Partitions implements Partitioning {

  private final PartitioningStatus status;
  private final ImmutableList<ImmutableSet<Property>> partitions;

  public static Partitions partitions(PartitioningStatus pStatus, ImmutableList<ImmutableSet<Property>> pPartitions) {
    return new Partitions(pStatus, pPartitions);
  }

  public static Partitions none() {
    return new Partitions(PartitioningStatus.NONE, ImmutableList.<ImmutableSet<Property>>of());
  }

  private Partitions(PartitioningStatus pStatus, ImmutableList<ImmutableSet<Property>> pPartitions) {
    status = Preconditions.checkNotNull(pStatus);
    partitions = Preconditions.checkNotNull(pPartitions);
  }

  @Override
  public PartitioningStatus getStatus() {
    return status;
  }

  @Override
  public ImmutableList<ImmutableSet<Property>> getPartitions() {
    return partitions;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof Partitioning)) {
      return false;
    }

    Partitioning p = (Partitioning) pObj;

    if (!p.getPartitions().equals(this.getPartitions())) {
      return false;
    }

    if (!p.getStatus().equals(this.getStatus())) {
      return false;
    }

    return true;
  }

  @Override
  public Partitioning substract(Set<Property> pProperties) {

    ImmutableList.Builder<ImmutableSet<Property>> result = ImmutableList.<ImmutableSet<Property>>builder();

    for (ImmutableSet<Property> p: partitions) {
      result.add(ImmutableSet.<Property>copyOf(Sets.difference(p, pProperties)));
    }

    return partitions(getStatus(), result.build());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + status.hashCode();
    result = prime * result + partitions.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s: %s", status, partitions);
  }

  @Override
  public boolean isEmpty() {
    if (partitions.isEmpty()) {
      return true;
    }

    for (ImmutableSet<Property> p: partitions) {
      if (!p.isEmpty()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int partitionCount() {
    return partitions.size();
  }

  @Override
  public Iterator<ImmutableSet<Property>> iterator() {
    return partitions.iterator();
  }

  @Override
  public Partitioning withoutFirst() {
    Preconditions.checkState(!this.isEmpty());

    return new Partitions(this.status, this.partitions.subList(1, this.partitions.size()));
  }

  @Override
  public ImmutableSet<Property> getFirstPartition() {
    Preconditions.checkState(!this.isEmpty());

    return partitions.iterator().next();
  }

}
