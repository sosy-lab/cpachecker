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

  /**
   * Creates a new builder for message content. The builder produces a flat map of key-value pairs,
   * where keys can be hierarchical using dot notation. Levels can be pushed and popped to create a
   * hierarchy.
   *
   * @return the new builder
   */
  public static ContentBuilder builder() {
    return new ContentBuilder();
  }

  /**
   * Pushes a new level to the hierarchy. If a key is added, it will be prefixed with the current
   * levels, separated by dots. For example, if the levels are ["level1", "level2"] and the key
   * "key" is added with value "value", the resulting entry will be "level1.level2.key=value".
   *
   * @param pLevel the name of the new level
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentBuilder pushLevel(String pLevel) {
    levels.add(pLevel);
    return this;
  }

  /**
   * Adds the given key-value pair to the content if the condition is true.
   *
   * @see #put(String, String)
   * @param pCondition the condition to check
   * @param pKey the key to add
   * @param pValue the value to add
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentBuilder putIf(boolean pCondition, String pKey, String pValue) {
    if (pCondition) {
      return put(pKey, pValue);
    }
    return this;
  }

  /**
   * Pops the last level from the hierarchy. If there are no levels, nothing happens.
   *
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentBuilder popLevel() {
    if (!levels.isEmpty()) {
      levels.removeLast();
    }
    return this;
  }

  /**
   * Adds the given key-value pair to the content. The key will be prefixed with the current levels,
   * separated by dots. For example, if the levels are ["level1", "level2"] and the key "key" is
   * added with value "value", the resulting entry will be "level1.level2.key=value".
   *
   * @param pKey the key to add
   * @param pValue the value to add
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentBuilder put(String pKey, String pValue) {
    String fullKey = Joiner.on(".").join(Collections3.listAndElement(levels, pKey));
    contentBuilder.put(fullKey, pValue);
    return this;
  }

  /**
   * Adds all entries from the given map to the content. Each key will be prefixed with the current
   * levels, separated by dots. For example, if the levels are ["level1", "level2"] and the map
   * contains the entry "key"="value", the resulting entry will be "level1.level2.key=value".
   *
   * @param pContent the map to add
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentBuilder putAll(Map<String, String> pContent) {
    for (Map.Entry<String, String> entry : pContent.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Builds the content map. If a key was added multiple times, the last value is kept.
   *
   * @return the built content map
   */
  public ImmutableMap<String, String> build() {
    return contentBuilder.buildKeepingLast();
  }
}
