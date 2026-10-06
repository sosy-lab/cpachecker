// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Provides cached location states for CFA nodes. */
public interface CachedLocationStateProvider {

  /** Returns the canonical state for a node of the CFA used to create this provider. */
  LocationState getState(CFANode pNode);
}
