// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * One possible step of the execution: the successor that the wrapped CPA computed and the edge that
 * leads to it.
 */
record ExecutionStep(CFAEdge edge, AbstractState successor) {

  ExecutionStep {
    checkNotNull(edge);
    checkNotNull(successor);
  }
}
