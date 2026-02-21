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

public class ContentReader {

  private final Map<String, String> content;
  private final Deque<String> level;

  private ContentReader(Map<String, String> pContent) {
    content = pContent;
    level = new ArrayDeque<>();
  }

  public static ContentReader read(Map<String, String> pContent) {
    return new ContentReader(pContent);
  }

  @CanIgnoreReturnValue
  public ContentReader pushLevel(String pLevel) {
    level.push(pLevel);
    return this;
  }

  @CanIgnoreReturnValue
  public ContentReader popLevel() {
    if (!level.isEmpty()) {
      level.pop();
    }
    return this;
  }

  public String get(String pKey) {
    return content.get(Joiner.on(".").join(listAndElement(level, pKey)));
  }

  public Map<String, String> getContent() {
    String prefix = Joiner.on(".").join(level);
    return content.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(prefix + "."))
        .collect(
            ImmutableMap.toImmutableMap(
                entry -> entry.getKey().substring(prefix.length() + 1), Entry::getValue));
  }
}
