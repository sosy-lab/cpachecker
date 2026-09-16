// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * An optional transfer trace for an ARG that preserves exact incoming paths. Following the
 * predecessor chain recovers edges skipped by composite edge aggregation. The trace describes one
 * transfer occurrence; it does not participate in semantic equality.
 */
public interface AbstractStateWithIncomingEdge extends AbstractState {

  @Nullable AbstractStateWithIncomingEdge getPredecessor();

  @Nullable CFAEdge getIncomingEdge();
}
