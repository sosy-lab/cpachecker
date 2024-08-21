// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.Map;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Classes implementing this interface derive interpolants of a whole path.
 *
 * @param <I> the type of interpolant created by the implementation
 */
public interface PathInterpolator<I extends Interpolant<?, I>> extends Statistics {

  Map<ARGState, I> performInterpolation(ARGPath errorPath, I interpolant)
      throws CPAException, InterruptedException;
}
