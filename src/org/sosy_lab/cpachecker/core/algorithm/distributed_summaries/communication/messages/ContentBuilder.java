// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class ContentBuilder {

  private final ImmutableMap.Builder<String, String> contentBuilder;

  private final Deque<String> level;

  private ContentBuilder(int pExpectedSize) {
    contentBuilder = ImmutableMap.builderWithExpectedSize(pExpectedSize);
    level = new ArrayDeque<>();
  }

  public static ContentBuilder builder() {
    // 4 is the default expected size (ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY)
    return new ContentBuilder(4);
  }

  public static ContentBuilder builderWithExpectedSize(int pExpectedSize) {
    return new ContentBuilder(pExpectedSize);
  }

  @CanIgnoreReturnValue
  public ContentBuilder pushLevel(String pLevel) {
    level.push(pLevel);
    return this;
  }

  @CanIgnoreReturnValue
  public ContentBuilder putIf(boolean pCondition, String pKey, String pValue) {
    if (pCondition) {
      return put(pKey, pValue);
    }
    return this;
  }

  @CanIgnoreReturnValue
  public ContentBuilder popLevel() {
    if (!level.isEmpty()) {
      level.pop();
    }
    return this;
  }

  @CanIgnoreReturnValue
  public ContentBuilder put(String pKey, String pValue) {
    String fullKey =
        Joiner.on(".").join(ImmutableList.<String>builder().addAll(level).add(pKey).build());
    contentBuilder.put(fullKey, pValue);
    return this;
  }

  @CanIgnoreReturnValue
  public ContentBuilder putAll(Map<String, String> pContent) {
    for (Map.Entry<String, String> entry : pContent.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public ImmutableMap<String, String> build() {
    return contentBuilder.buildKeepingLast();
  }
}
