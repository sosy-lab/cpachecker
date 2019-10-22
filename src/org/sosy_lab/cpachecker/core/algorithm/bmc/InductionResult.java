/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;

public class InductionResult<T extends CandidateInvariant> extends ProofResult {

  private final @Nullable T invariantAbstraction;

  private final Set<SymbolicCandiateInvariant> badStateBlockingClauses;

  private final int k;

  private InductionResult(T pInvariantAbstraction) {
    super(true);
    invariantAbstraction = Objects.requireNonNull(pInvariantAbstraction);
    badStateBlockingClauses = ImmutableSet.of();
    k = -1;
  }

  private InductionResult(
      Iterable<? extends SymbolicCandiateInvariant> pBadStateBlockingClauses,
      int pK) {
    super(false);
    checkArgument(
        !Iterables.isEmpty(pBadStateBlockingClauses),
        "Bad-state blocking invariants should be present if (and only if) induction failed.");
    if (pK < 0) {
      throw new IllegalArgumentException(
          "k must not be negative for failed induction results, but is " + pK);
    }
    invariantAbstraction = null;
    badStateBlockingClauses = ImmutableSet.copyOf(pBadStateBlockingClauses);
    k = pK;
  }

  @Override
  public boolean isSuccessful() {
    assert super.isSuccessful() == (invariantAbstraction != null);
    return invariantAbstraction != null;
  }

  public T getInvariantRefinement() {
    checkArgument(
        isSuccessful(), "An invariant abstraction is only present if induction succeeded.");
    return invariantAbstraction;
  }

  public Set<SymbolicCandiateInvariant> getBadStateBlockingClauses() {
    checkState(
        !isSuccessful(),
        "Auxiliary-invariants for blocking bad states are only available if induction failed.");
    assert !badStateBlockingClauses.isEmpty();
    return badStateBlockingClauses;
  }

  public int getK() {
    checkState(!isSuccessful(), "Input-assignment length is only present if induction failed.");
    return k;
  }

  public static <T extends CandidateInvariant> InductionResult<T> getSuccessful(
      T pInvariantAbstraction) {
    return new InductionResult<>(pInvariantAbstraction);
  }

  public static <T extends CandidateInvariant> InductionResult<T> getFailed(
      Iterable<? extends SymbolicCandiateInvariant> pBadStateBlockingClauses,
      int pK) {
    return new InductionResult<>(pBadStateBlockingClauses, pK);
  }
}
