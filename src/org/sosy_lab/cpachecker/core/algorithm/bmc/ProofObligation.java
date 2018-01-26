/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public abstract class ProofObligation
    implements Iterable<ProofObligation>, Comparable<ProofObligation> {

  private final SymbolicCandiateInvariant blockedAbstractCti;

  private final Optional<SymbolicCandiateInvariant> blockedConcreteCti;

  private final int frameIndex;

  private final int nSpuriousTransitions;

  private final int length;

  private ProofObligation(
      SymbolicCandiateInvariant pAbstractBlockingClause,
      Optional<SymbolicCandiateInvariant> pConcreteBlockingClause,
      int pFrameIndex,
      int pNSpuriousTransitions,
      int pLength) {
    blockedAbstractCti = Objects.requireNonNull(pAbstractBlockingClause);
    blockedConcreteCti = Objects.requireNonNull(pConcreteBlockingClause);
    if (pFrameIndex < 0) {
      throw new IllegalArgumentException("Frame index must not be negative, but is " + pFrameIndex);
    }
    frameIndex = pFrameIndex;
    if (pNSpuriousTransitions < 0) {
      throw new IllegalArgumentException(
          "Number of spurious transitions must not be negative, but is " + pNSpuriousTransitions);
    }
    nSpuriousTransitions = pNSpuriousTransitions;
    if (pLength < 1) {
      throw new IllegalArgumentException("Length must be positive but is " + pLength);
    }
    length = pLength;
  }

  private static class NonLeafProofObligation extends ProofObligation {

    private final ProofObligation cause;

    private NonLeafProofObligation(
        SymbolicCandiateInvariant pAbstractBlockingClause,
        Optional<SymbolicCandiateInvariant> pConcreteBlockingClause,
        int pNSpuriousTransitions,
        int pLength,
        ProofObligation pCause) {
      super(
          pAbstractBlockingClause,
          pConcreteBlockingClause,
          pCause.getFrameIndex() - 1,
          pNSpuriousTransitions,
          pLength);
      cause = Objects.requireNonNull(pCause);
    }

    @Override
    public Optional<ProofObligation> getCause() {
      return Optional.of(cause);
    }

    @Override
    public CandidateInvariant getViolatedInvariant() {
      return aggregateOverTrace(Function.identity(), (a, b) -> b).getViolatedInvariant();
    }

    @Override
    public int getLiftingAbstractionFailureCount() {
      return aggregateOverTrace(o -> o.getBlockedConcreteCti().isPresent() ? 1 : 0, Integer::sum);
    }

    private <T> T aggregateOverTrace(
        Function<ProofObligation, T> pMap, BinaryOperator<T> pAccumulator) {
      ProofObligation current = this;
      T accumulated = pMap.apply(current);
      while (current.getCause().isPresent()) {
        current = current.getCause().get();
        accumulated = pAccumulator.apply(accumulated, pMap.apply(current));
      }
      return accumulated;
    }

    @Override
    public Iterator<ProofObligation> iterator() {
      return new Iterator<ProofObligation>() {

        private Optional<ProofObligation> current = Optional.of(NonLeafProofObligation.this);

        @Override
        public boolean hasNext() {
          return current.isPresent();
        }

        @Override
        public ProofObligation next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          ProofObligation result = current.get();
          current = result.getCause();
          return result;
        }
      };
    }

    @Override
    public ProofObligation addSpuriousTransition() {
      return new NonLeafProofObligation(
          getBlockedAbstractCti(),
          getBlockedConcreteCti(),
          getNSpuriousTransitions() + 1,
          getLength(),
          cause);
    }

    @Override
    public ProofObligation incrementFrameIndex() {
      return new NonLeafProofObligation(
          getBlockedAbstractCti(),
          getBlockedConcreteCti(),
          getNSpuriousTransitions(),
          getLength(),
          cause.incrementFrameIndex());
    }

    @Override
    public Optional<ProofObligation> find(Predicate<? super ProofObligation> pFilter) {
      return StreamSupport.stream(spliterator(), false).filter(pFilter).findFirst();
    }

  }

  private static class LeafProofObligation extends ProofObligation {

    private final CandidateInvariant violatedInvariant;

    private LeafProofObligation(
        SymbolicCandiateInvariant pBlockedAbstractCti,
        Optional<SymbolicCandiateInvariant> pBlockedConcreteCti,
        int pFrameIndex,
        int pNSpuriousTransitions,
        int pLength,
        CandidateInvariant pViolatedInvariant) {
      super(
          pBlockedAbstractCti,
          pBlockedConcreteCti,
          pFrameIndex,
          pNSpuriousTransitions,
          pLength);
      violatedInvariant = Objects.requireNonNull(pViolatedInvariant);
    }

    @Override
    public Optional<ProofObligation> getCause() {
      return Optional.empty();
    }

    @Override
    public CandidateInvariant getViolatedInvariant() {
      return violatedInvariant;
    }

    @Override
    public ProofObligation addSpuriousTransition() {
      return new LeafProofObligation(
          getBlockedAbstractCti(),
          getBlockedConcreteCti(),
          getFrameIndex(),
          getNSpuriousTransitions() + 1,
          getLength(),
          violatedInvariant);
    }

    @Override
    public ProofObligation incrementFrameIndex() {
      return new LeafProofObligation(
          getBlockedAbstractCti(),
          getBlockedConcreteCti(),
          getFrameIndex() + 1,
          getNSpuriousTransitions(),
          getLength(),
          getViolatedInvariant());
    }

    @Override
    public int getLiftingAbstractionFailureCount() {
      return getBlockedConcreteCti().isPresent() ? 1 : 0;
    }

    @Override
    public Iterator<ProofObligation> iterator() {
      return Iterators.singletonIterator(this);
    }

    @Override
    public Optional<ProofObligation> find(Predicate<? super ProofObligation> pFilter) {
      if (pFilter.test(this)) {
        return Optional.of(this);
      }
      return Optional.empty();
    }
  }

  public SymbolicCandiateInvariant getBlockedAbstractCti() {
    return blockedAbstractCti;
  }

  public Optional<SymbolicCandiateInvariant> getBlockedConcreteCti() {
    return blockedConcreteCti;
  }

  @Override
  public int compareTo(ProofObligation pOther) {
    ComparisonChain compChain =
        ComparisonChain.start()
            .compare(getFrameIndex(), pOther.frameIndex)
            .compareFalseFirst(getCause().isPresent(), pOther.getCause().isPresent());
    if (getCause().isPresent()) {
      compChain = compChain.compare(getCause().get(), pOther.getCause().get());
    }
    return compChain
        .compare(getNSpuriousTransitions(), pOther.getNSpuriousTransitions())
        .compare(getLiftingAbstractionFailureCount(), pOther.getLiftingAbstractionFailureCount())
        .result();
  }

  public abstract Optional<ProofObligation> getCause();

  public abstract CandidateInvariant getViolatedInvariant();

  public abstract ProofObligation incrementFrameIndex();

  public abstract int getLiftingAbstractionFailureCount();

  public abstract Optional<ProofObligation> find(Predicate<? super ProofObligation> pFilter);

  public int getFrameIndex() {
    return frameIndex;
  }

  public int getNSpuriousTransitions() {
    return nSpuriousTransitions;
  }

  public int getLength() {
    return length;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof ProofObligation) {
      ProofObligation other = (ProofObligation) pOther;
      return frameIndex == other.frameIndex
          && nSpuriousTransitions == other.nSpuriousTransitions
          && blockedConcreteCti.equals(other.blockedConcreteCti)
          && blockedAbstractCti.equals(other.blockedAbstractCti)
          && length == other.length
          && getViolatedInvariant().equals(other.getViolatedInvariant());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        frameIndex,
        nSpuriousTransitions,
        blockedConcreteCti,
        blockedAbstractCti,
        length,
        getViolatedInvariant());
  }

  @Override
  public String toString() {
    return String.format(
        "(%s, [%s], %d, %d)",
        blockedAbstractCti,
        blockedConcreteCti.isPresent() ? blockedConcreteCti.toString() : "",
        frameIndex,
        nSpuriousTransitions);
  }

  public abstract ProofObligation addSpuriousTransition();

  public ProofObligation causeProofObligation(
      SymbolicCandiateInvariant pBlockedAbstractCti,
      Optional<SymbolicCandiateInvariant> pBlockedConcreteCti,
      int pNSpuriousTransitions,
      int pLength) {
    return new NonLeafProofObligation(
        pBlockedAbstractCti,
        pBlockedConcreteCti,
        pNSpuriousTransitions,
        pLength,
        this);
  }

  public static ProofObligation createObligation(
      SymbolicCandiateInvariant pBlockedAbstractCti,
      Optional<SymbolicCandiateInvariant> pBlockedConcreteCti,
      int pFrameIndex,
      int pNSpuriousTransitions,
      int pLength,
      CandidateInvariant pViolatedInvariant) {
    return new LeafProofObligation(
        pBlockedAbstractCti,
        pBlockedConcreteCti,
        pFrameIndex,
        pNSpuriousTransitions,
        pLength,
        pViolatedInvariant);
  }
}
