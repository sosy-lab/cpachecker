// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface Splitable {

  /**
   * Returns a version of this where the passed states are used as replacements. The returned state
   * is allowed to be identical to this in case there are no changes.
   *
   * @param pReplacementStates states that shall be used in the forked as replacement for states in
   *     the old state
   */
  AbstractState forkWithReplacements(Collection<AbstractState> pReplacementStates);
}
