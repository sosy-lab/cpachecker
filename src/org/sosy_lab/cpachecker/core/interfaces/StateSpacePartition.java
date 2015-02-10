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
package org.sosy_lab.cpachecker.core.interfaces;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

/**
 * Interface that describes a partition of the state space.
 *
 * Two partitions are equivalent if their partition key is equivalent (see interface {@link Partitionable}).
 *
 * (A class is more flexible than an enum:
 *    own instances of StateSpacePartition can be created in the project-specific code.)
 *
 */
public class StateSpacePartition implements Partitionable {

  private static final StateSpacePartition defaultPartition = getPartitionWithKey(Integer.valueOf(0));

  public static StateSpacePartition getDefaultPartition() {
    return defaultPartition;
  }

  public static synchronized StateSpacePartition getPartitionWithKey(final Object pPartitionKey) {
    return new StateSpacePartition(pPartitionKey);
  }

  private final @Nonnull Object partitionKey;

  private StateSpacePartition(Object pPartitionKey) {
    Preconditions.checkNotNull(pPartitionKey);
    this.partitionKey = pPartitionKey;
  }

  @Override
  public Object getPartitionKey() {
    return partitionKey;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof StateSpacePartition)) {
      return false;
    }
    return ((StateSpacePartition) pObj).getPartitionKey().equals(partitionKey);
  }

  @Override
  public String toString() {
    return partitionKey.toString();
  }

  @Override
  public int hashCode() {
    return partitionKey.hashCode();
  }

}
