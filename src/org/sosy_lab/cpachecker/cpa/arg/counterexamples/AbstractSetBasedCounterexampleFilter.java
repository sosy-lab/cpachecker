// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.ForOverride;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * Abstract base implementation of {@link CounterexampleFilter}. This implementation stores a
 * representation of each previously found counterexample in a set, and reports counterexamples as
 * relevant if their representation was not already contained in that set.
 *
 * <p>The representation of each counterexample is left to be determined by the sub-classes. The
 * general guidelines of {@link CounterexampleFilter} apply: the representation should be immutable,
 * should not retain too much memory, and thus should not contain ARGStates.
 *
 * <p>Formally speaking, this class defines counterexamples as irrelevant if their representation
 * (as given by {@link #getCounterexampleRepresentation(CounterexampleInfo)} is equal (as defined by
 * {@link Object#equals(Object)}) to a previously found counterexamples.
 *
 * @param <T> The type of the representation of counterexamples.
 */
public abstract class AbstractSetBasedCounterexampleFilter<T> implements CounterexampleFilter {

  private final Set<T> foundCounterexamples = new HashSet<>();

  protected AbstractSetBasedCounterexampleFilter(
      Configuration config, LogManager logger, ConfigurableProgramAnalysis cpa) {
    // We do not need the parameter objects,
    // this constructor is only to encourage sub-classes
    // to define a constructor with the same signature
    // (each CounterexampleFilter needs to have one such constructor).
    checkNotNull(config);
    checkNotNull(logger);
    checkNotNull(cpa);
  }

  @Override
  public boolean isRelevant(CounterexampleInfo counterexample) throws InterruptedException {
    Optional<T> representation = getCounterexampleRepresentation(checkNotNull(counterexample));
    if (!representation.isPresent()) {
      return true;
    }

    boolean setChanged = foundCounterexamples.add(representation.orElseThrow());
    return setChanged; // relevant <=> new cex <=> set changed
  }

  /**
   * This method needs to produce an immutable representation of each counterexample. The more
   * abstract it is, the more "similar" counterexamples are reported as irrelevant. If this filter
   * does not manage to produce a meaningful representation of the current path, it may return
   * {@link Optional#empty()}. In this case, the counterexample is considered relevant.
   *
   * @param counterexample A counterexample, guaranteed to be not null.
   * @return An immutable representation of the counterexample, needs to have proper implementations
   *     of {@link Object#equals(Object)} and {@link Object#hashCode()}, or {@link
   *     Optional#empty()}.
   */
  @ForOverride
  protected abstract Optional<T> getCounterexampleRepresentation(CounterexampleInfo counterexample)
      throws InterruptedException;
}
