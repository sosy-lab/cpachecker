// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Interface that describes a partition of the state space.
 *
 * <p>Two partitions are equivalent if their partition key is equivalent (see interface {@link
 * Partitionable}).
 *
 * <p>(A class is more flexible than an enum: own instances of StateSpacePartition can be created in
 * the project-specific code.)
 */
public class StateSpacePartition implements Partitionable {

  private static final StateSpacePartition defaultPartition = getPartitionWithKey(0);

  public static StateSpacePartition getDefaultPartition() {
    return defaultPartition;
  }

  public static synchronized StateSpacePartition getPartitionWithKey(final Object pPartitionKey) {
    return new StateSpacePartition(pPartitionKey);
  }

  private final @NonNull Object partitionKey;

  private StateSpacePartition(Object pPartitionKey) {
    Preconditions.checkNotNull(pPartitionKey);
    partitionKey = pPartitionKey;
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
