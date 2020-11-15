// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public interface LoopIterationReportingState extends AbstractState, Partitionable {

  int getIteration(Loop pLoop);

  int getDeepestIteration();

  Set<Loop> getDeepestIterationLoops();
}
