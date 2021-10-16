// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import org.sosy_lab.cpachecker.cfa.CFA;

/** Contains the status of an array abstraction and its resulting transformed CFA. */
public final class ArrayAbstractionResult {

  private final Status status;
  private final CFA transformedCfa;

  ArrayAbstractionResult(Status pStatus, CFA pTransformedCfa) {
    status = pStatus;
    transformedCfa = pTransformedCfa;
  }

  /**
   * Returns the status of this array abstraction result.
   *
   * @return the status of this array abstraction result
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Returns the transformed CFA of this array abstraction result.
   *
   * <p>The status of this array abstraction result should be considered before the returned CFA is
   * used.
   *
   * @return the transformed CFA of this array abstraction result
   */
  public CFA getTransformedCfa() {
    return transformedCfa;
  }

  /**
   * The status of an array abstraction result which contains information about its success and
   * precision.
   */
  public enum Status {

    /** The array abstraction succeeded and the result is precise. */
    PRECISE,

    /** The array abstraction succeeded but the result is imprecise. */
    IMPRECISE,

    /** The array abstraction didn't succeed. */
    FAILED;
  }
}
