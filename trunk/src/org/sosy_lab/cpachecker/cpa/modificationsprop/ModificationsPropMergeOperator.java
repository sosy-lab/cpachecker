// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import java.util.Arrays;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Non-standard merge-join operator only merging if the required values are equal. These are the
 * program locations and the stack tracked.
 */
public class ModificationsPropMergeOperator implements MergeOperator {

  /** Merge operator constructor. See class info for details. */
  public ModificationsPropMergeOperator() {}

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {
    // This operator may only be applied to D elements.
    @SuppressWarnings("unchecked")
    ModificationsPropState el1D = (ModificationsPropState) el1;
    @SuppressWarnings("unchecked")
    ModificationsPropState el2D = (ModificationsPropState) el2;
    if (el1D.getLocationInModCfa().equals(el2D.getLocationInModCfa())
        && el1D.getLocationInOriginalCfa().equals(el2D.getLocationInOriginalCfa())
        && Arrays.equals(el1D.getOriginalStack().toArray(), el2D.getOriginalStack().toArray())) {
      return el1D.join(el2D);
    } else {
      return el2;
    }
  }
}
