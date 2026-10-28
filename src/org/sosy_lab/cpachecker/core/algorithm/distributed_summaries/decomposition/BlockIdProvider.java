// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sosy_lab.common.UniqueIdGenerator;

/** Assigns a short, unique ID to every distinct key it sees. */
public final class BlockIdProvider {

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final Map<Object, String> idOf = new ConcurrentHashMap<>();

  public String idFor(Object pKey) {
    return idOf.computeIfAbsent(pKey, unused -> Integer.toString(idGenerator.getFreshId()));
  }
}
