// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for methods to generate invariants about the program.
 *
 * <p>First {@link #start(CFANode)} needs to be called with the entry point of the CFA, and then
 * {@link #getSupplier()} or {@link #getExpressionTreeSupplier()} can be called to retrieve the
 * reached set with the invariants.
 *
 * <p>It is a good idea to call {@link #start(CFANode)} as soon as possible and {@link
 * #getSupplier()} or {@link #getExpressionTreeSupplier()} as late as possible to minimize waiting
 * times if the generator is configured for asynchronous execution.
 *
 * <p>It is also a good idea to call {@link #getSupplier()} and {@link #getExpressionTreeSupplier()}
 * only if really necessary (in synchronous case, it is expensive).
 */
public interface InvariantGenerator {

  /**
   * Checks if the invariant generator has already been started.
   *
   * @return {@code true} if the invariant generator has already been started, {@code false}
   *     otherwise.
   */
  boolean isStarted();

  /** Prepare invariant generation, and optionally start the algorithm. May be called only once. */
  void start(CFANode initialLocation);

  /**
   * Cancel the invariant generation algorithm, if running. Can be called only after {@link
   * #start(CFANode)} was called.
   */
  void cancel();

  /**
   * Retrieve an {@link InvariantSupplier} that contains the currently available invariants.
   *
   * <p>It is possible that in the future further invariants become available and this method
   * returns another supplier.
   *
   * <p>Can be called only after {@link #start(CFANode)} was called.
   *
   * <p>Depending on the invariant generator, this method may either block for some time during the
   * invariant generation runs, or return a current snapshot of the invariants quickly.
   *
   * @throws CPAException If the invariant generation failed.
   * @throws InterruptedException If the invariant generation was interrupted.
   */
  InvariantSupplier getSupplier() throws CPAException, InterruptedException;

  /**
   * Retrieve an {@link ExpressionTreeSupplier} that contains the currently available invariants.
   *
   * <p>It is possible that in the future further invariants become available and this method
   * returns another supplier.
   *
   * <p>Can be called only after {@link #start(CFANode)} was called.
   *
   * <p>Depending on the invariant generator, this method may either block for some time during the
   * invariant generation runs, or return a current snapshot of the invariants quickly.
   *
   * @throws CPAException If the invariant generation failed.
   * @throws InterruptedException If the invariant generation was interrupted.
   */
  ExpressionTreeSupplier getExpressionTreeSupplier() throws CPAException, InterruptedException;

  /**
   * Return whether the invariant generation has already proved that the specification holds, and no
   * further checks are necessary. If possible, this method should be cheap.
   */
  boolean isProgramSafe();
}
