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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.Math.max;

import com.google.common.collect.Maps;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.common.collect.PersistentSortedMaps;
import org.sosy_lab.common.collect.PersistentSortedMaps.MergeConflictHandler;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

public final class Targets {
  public static class SerializationProxy implements Serializable {
    public SerializationProxy(Targets targets) {
      Map<String, List<PointerTarget>> targetsTargets = Maps.newHashMapWithExpectedSize(targets.targets.size());
      for(Entry<String, PersistentList<PointerTarget>> entry : targets.targets.entrySet()) {
        targetsTargets.put(entry.getKey(), new ArrayList<>(entry.getValue()));
      }
      Map<String, Integer> bucketSizes = Maps.newHashMapWithExpectedSize(targets.bucketSizes.size());
      for(Entry<String, Integer> entry : targets.bucketSizes.entrySet()) {
        bucketSizes.put(entry.getKey(), entry.getValue());
      }
      this.targets = targetsTargets;
      this.bucketSizes = bucketSizes;
      this.maxBucketSize = targets.maxBucketSize;
    }

    public Object readResolve() {
      Map<String, PersistentList<PointerTarget>> targets = Maps.newHashMapWithExpectedSize(this.targets.size());
      for (Entry<String, List<PointerTarget>> entry : this.targets.entrySet()) {
        targets.put(entry.getKey(), PersistentLinkedList.copyOf(entry.getValue()));
      }
      Map<String, Integer> bucketSizes = Maps.newHashMapWithExpectedSize(this.bucketSizes.size());
      for (Entry<String, Integer> entry : bucketSizes.entrySet()) {
        bucketSizes.put(entry.getKey(), entry.getValue());
      }
      return new Targets(PathCopyingPersistentTreeMap.copyOf(targets),
                         PathCopyingPersistentTreeMap.copyOf(bucketSizes),
                         maxBucketSize);
    }

    private static final long serialVersionUID = -2997951960927643153L;
    private final Map<String, List<PointerTarget>> targets;
    private final Map<String, Integer> bucketSizes;
    private final int maxBucketSize;
  }

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   * @param in the input stream
   */
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private Targets (final PersistentSortedMap<String, PersistentList<PointerTarget>> targets,
                   final PersistentSortedMap<String, Integer> bucketSizes,
                   final int maxBucketSize) {
    this.targets = targets;
    this.bucketSizes = bucketSizes;
    this.maxBucketSize = maxBucketSize;
  }

  private static final boolean isGlobal(final String ufName) {
    return ufName.contains("global");
  }

  public static @Nonnull Targets emptyTargets() {
    return EMPTY_INSTANCE;
  }

  public @Nonnull Targets add(final @Nonnull String ufName, final @Nonnull PointerTarget target) {
    final PersistentList<PointerTarget> ufTargets = firstNonNull(targets.get(ufName),
                                                                      PersistentLinkedList.<PointerTarget>of());
    final int ufSize = firstNonNull(bucketSizes.get(ufName), 0) + 1;
    return new Targets(targets.putAndCopy(ufName, ufTargets.with(target)),
                       bucketSizes.putAndCopy(ufName, ufSize),
                       !isGlobal(ufName) ? max(ufSize, maxBucketSize) : maxBucketSize);
  }

  public @Nonnull PersistentList<PointerTarget> getAll(final @Nonnull String ufName) {
    return firstNonNull(targets.get(ufName),
                        PersistentLinkedList.<PointerTarget>of());
  }

  public int getTargetsCount(final @Nonnull String ufName) {
    return firstNonNull(bucketSizes.get(ufName), 0);
  }

  public int getMaxTargetCount() {
    return maxBucketSize;
  }

  public boolean isEmpty() {
    return targets.isEmpty();
  }

  /**
   * Gives a handler for merge conflicts.
   *
   * @param <K> The type of the keys in the merge conflict handler.
   * @param <T> The type of the list entries in the merge conflict handler.
   * @return A handler for merge conflicts.
   */
  private static <K, T> MergeConflictHandler<K, PersistentList<T>> mergeOnConflict() {
    return new MergeConflictHandler<K, PersistentList<T>>() {
      @Override
      public PersistentList<T> resolveConflict(K pKey, PersistentList<T> pList1, PersistentList<T> pList2) {
        return DeferredAllocationPool.mergeLists(pList1, pList2);
      }
    };
  }

  private static <K> MergeConflictHandler<K, Integer> countOnConflict(
      final @Nonnull PersistentSortedMap<String, PersistentList<PointerTarget>> targets) {
    return new MergeConflictHandler<K, Integer>() {
      @Override
      public Integer resolveConflict(K pKey, Integer n1, Integer n2) {
        return targets.get(pKey).size();
      }
    };
  }

  public static @Nonnull Targets merge(final @Nonnull Targets targets1, final @Nonnull Targets targets2) {
    final PersistentSortedMap<String, PersistentList<PointerTarget>> targets =
                       PersistentSortedMaps.merge(targets1.targets, targets2.targets,
                                                  Targets.<String, PointerTarget>mergeOnConflict());
    final PersistentSortedMap<String, Integer> bucketSizes =
                       PersistentSortedMaps.merge(targets1.bucketSizes, targets2.bucketSizes,
                                                  Targets.<String>countOnConflict(targets));

    int maxSize = 0;
    for (Entry<String, Integer> e : bucketSizes.entrySet()) {
      final int v = e.getValue();
      if (v > maxSize && !isGlobal(e.getKey())){
        maxSize = v;
      }
    }
    return new Targets(targets, bucketSizes, maxSize);
  }

  private final PersistentSortedMap<String, PersistentList<PointerTarget>> targets;
  private final PersistentSortedMap<String, Integer> bucketSizes;
  private final int maxBucketSize;

  private static final Targets EMPTY_INSTANCE = new Targets(
      PathCopyingPersistentTreeMap.<String, PersistentList<PointerTarget>>of(),
      PathCopyingPersistentTreeMap.<String, Integer>of(),
      0);
}
