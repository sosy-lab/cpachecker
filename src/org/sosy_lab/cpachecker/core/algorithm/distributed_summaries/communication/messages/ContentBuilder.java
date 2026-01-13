// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.Collections3;

public class ContentBuilder {

  private final ImmutableMap.Builder<String, String> contentBuilder;

  private final List<String> levels;

  private ContentBuilder() {
    contentBuilder = ImmutableMap.builder();
    levels = new ArrayList<>();
  }

  public static ContentBuilder builder() {
    return new ContentBuilder();
  }

  @CanIgnoreReturnValue
  public ContentBuilder pushLevel(String pLevel) {
    levels.add(pLevel);
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
    if (!levels.isEmpty()) {
      levels.removeLast();
    }
    return this;
  }

  @CanIgnoreReturnValue
  public ContentBuilder put(String pKey, String pValue) {
    String fullKey = Joiner.on(".").join(Collections3.listAndElement(levels, pKey));
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
