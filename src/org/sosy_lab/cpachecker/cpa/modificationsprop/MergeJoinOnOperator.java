// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.ImmutableSet;
import java.util.function.Function;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Standard merge-join operator only merging if the provided functions have equal results.
 *
 * @param <D> The Abstract State subclass used when using this merge operator. Make sure that merge
 *     is not called for non-D-elements.
 */
public class MergeJoinOnOperator<D extends AbstractState> implements MergeOperator {
  final AbstractDomain domain;
  final ImmutableSet<Function<D, Object>> mergeDomainGetters;

  /**
   * Creates a merge-join operator, based on the given join operator. Merges if function results are
   * equal only.
   */
  public MergeJoinOnOperator(
      AbstractDomain pDomain, ImmutableSet<Function<D, Object>> pMergeDomainGetters) {
    mergeDomainGetters = pMergeDomainGetters;
    domain = pDomain;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {
    // This operator may only be applied to D elements.
    @SuppressWarnings("unchecked")
    D el1D = (D) el1;
    @SuppressWarnings("unchecked")
    D el2D = (D) el2;
    if (mergeDomainGetters.stream().allMatch(get -> get.apply(el1D).equals(get.apply(el2D)))) {
      return domain.join(el1, el2);
    } else {
      return el2;
    }
  }
}
