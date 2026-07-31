// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

public class StringId implements DssIndexable {

  private final String id;

  private StringId(String pId) {
    id = pId;
  }

  public static StringId of(String id) {
    return new StringId(id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StringId stringId && id.equals(stringId.id);
  }

  @Override
  public String toString() {
    return id;
  }
}
