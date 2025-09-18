// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Reader for message content. The content is a flat map of key-value pairs, where keys can be
 * hierarchical using dot notation. Levels can be pushed and popped to create a hierarchy.
 */
public class ContentReader {

  private final Map<String, String> content;
  private final Deque<String> level;

  private ContentReader(Map<String, String> pContent) {
    content = pContent;
    level = new ArrayDeque<>();
  }

  /**
   * Creates a new reader for the given content.
   *
   * @param pContent the content to read
   * @return the new reader
   */
  public static ContentReader read(Map<String, String> pContent) {
    return new ContentReader(pContent);
  }

  /**
   * Pushes a new level to the hierarchy. If a key is about to be read, it will be prefixed with the
   * current levels, separated by dots. For example, if the levels are ["level1", "level2"] and the
   * key "key" is read, the resulting key will be "level1.level2.key".
   *
   * @param pLevel the level to push
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentReader pushLevel(String pLevel) {
    level.push(pLevel);
    return this;
  }

  /**
   * Pops the current level from the hierarchy. If there is no level to pop, nothing happens.
   *
   * @return this builder
   */
  @CanIgnoreReturnValue
  public ContentReader popLevel() {
    if (!level.isEmpty()) {
      level.pop();
    }
    return this;
  }

  /**
   * Gets the value for the given key, prefixed with the current levels. If the key does not exist,
   * null is returned.
   *
   * <p>Example: if the levels are ["level1", "level2"] and the key "key" is requested, the
   * resulting key will be "level1.level2.key".
   *
   * @param pKey the key to get
   * @return the value for the key, or null if the key does not exist
   */
  public String get(String pKey) {
    return content.get(Joiner.on(".").join(listAndElement(level, pKey)));
  }

  /**
   * Gets all key-value pairs that are prefixed with the current levels. The keys in the resulting
   * map will not contain the prefix.
   *
   * <p>Example: if the levels are ["level1", "level2"] and the content contains the entries
   * "level1.level2.key1"="value1" and "level1.level2.key2"="value2", the resulting map will contain
   * the entries "key1"="value1" and "key2"="value2".
   *
   * @return a map of all key-value pairs with the current level as prefix
   */
  public Map<String, String> getContent() {
    String prefix = Joiner.on(".").join(level);
    return content.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(prefix + "."))
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey().substring(prefix.length() + 1), Entry::getValue));
  }
}
