// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.Deque;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Classes implementing this interface are able to derive interpolants from edges. */
public interface EdgeInterpolator<S extends ForgetfulState<?>, I extends Interpolant<S, I>> {

  I deriveInterpolant(
      ARGPath errorPath,
      CFAEdge currentEdge,
      Deque<S> callstack,
      PathPosition offset,
      I inputInterpolant)
      throws CPAException, InterruptedException;

  int getNumberOfInterpolationQueries();
}
