// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Classes implementing this interface are able to create interpolants out of subtypes of {@link
 * AbstractState}.
 */
public interface InterpolantManager<S, I extends Interpolant<S, I>> {

  I createInitialInterpolant();

  I createInterpolant(S state);

  I getTrueInterpolant();

  I getFalseInterpolant();
}
