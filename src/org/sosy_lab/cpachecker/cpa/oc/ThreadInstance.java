// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * One dynamic thread instance of the ordering-consistency exploration. Instances are identified by
 * (creating instance, started function, number of previous starts of that function on the creating
 * path), so the same {@code pthread_create} in exclusive branches maps to a single instance, while
 * consecutive creates of the same function map to distinct instances. All create events that can
 * start this instance are recorded; the instance's creation guard is their guards' disjunction.
 */
public final class ThreadInstance {

  /** Identity of a thread instance before an id is assigned. */
  public record InstanceKey(int creatorId, String functionName, int ordinal) {}

  public static final int MAIN_INSTANCE_ID = 0;

  private final int id;
  private final InstanceKey key;
  private final List<Integer> createEventIds = new ArrayList<>();

  ThreadInstance(int pId, InstanceKey pKey) {
    id = pId;
    key = pKey;
  }

  public int getId() {
    return id;
  }

  public InstanceKey getKey() {
    return key;
  }

  public String getFunctionName() {
    return key.functionName();
  }

  /** Ids of all CREATE events that start this instance; empty for the main thread. */
  public ImmutableList<Integer> getCreateEventIds() {
    return ImmutableList.copyOf(createEventIds);
  }

  void addCreateEvent(int pEventId) {
    createEventIds.add(pEventId);
  }
}
