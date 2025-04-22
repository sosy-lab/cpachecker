// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public interface ReachedSetNotEqual {

  void setReachedSet(ReachedSet reachedSet);
}
