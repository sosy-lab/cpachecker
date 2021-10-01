// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public interface PathRestorator {

  public ARGPath computePath(ARGState pLastElement, List<AbstractState> pStack);

  public ARGPath computePath(
      ARGState pLastElement,
      Set<List<Integer>> pRefinedStates,
      List<AbstractState> pStack);

  public PathIterator iterator(ARGState pTarget, List<AbstractState> pStack);

}
