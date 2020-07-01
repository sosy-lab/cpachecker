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
package org.sosy_lab.cpachecker.cpa.splitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public abstract class SplitInfoState implements AbstractQueryableState {

  public abstract SplitInfoState getSplitPart(int numSplitParts, int splitPart);

  public abstract boolean isInSplit(int splitIndex);

  public abstract boolean isOneElementalSplit();

  public abstract SplitInfoState removeFromSplit(int removeIndex);

  public abstract SplitInfoState removeFromSplit(Collection<Integer> removeIndices);

  @Override
  public String getCPAName() {
    return "SplitterCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    if (property.equals("isSingle")) {
      return isOneElementalSplit();
    }
    throw new InvalidQueryException("The Query \"" + property + "\" is invalid.");
  }

  static final class SequenceSplitInfoState extends SplitInfoState {

    private final int[] inSplit;

    SequenceSplitInfoState(final int numberOfMaximalSplits) {
      inSplit = new int[numberOfMaximalSplits];

      for (int i = 0; i < numberOfMaximalSplits; i++) {
        inSplit[i] = i;
      }
    }

    private SequenceSplitInfoState(final int[] pInSplit) {
      inSplit = pInSplit;
    }

    @Override
    public boolean isInSplit(final int splitIndex) {
      return inSplit.length > 0
          && inSplit[0] <= splitIndex
          && inSplit[inSplit.length - 1] >= splitIndex;
    }

    @Override
    public SplitInfoState getSplitPart(final int numSplitParts, final int splitPart) {
      Preconditions.checkArgument(
          numSplitParts > 0, "Number of split parts must be a positive number.");
      Preconditions.checkArgument(
          splitPart >= 0 && splitPart < numSplitParts,
          "The split part must be in [0,numSplitParts).");

      if(inSplit.length < numSplitParts) {
        return this;
      }

      int minElem = inSplit.length / numSplitParts;
      int numAdditionalElem = inSplit.length % numSplitParts;

      return new SequenceSplitInfoState(
          Arrays.copyOfRange(
              inSplit,
              splitPart * minElem + (splitPart < numAdditionalElem ? splitPart : numAdditionalElem),
              (splitPart + 1) * minElem
                  + ((splitPart + 1) < numAdditionalElem ? splitPart + 1 : numAdditionalElem)));
    }

    @Override
    public SplitInfoState removeFromSplit(int pSplitIndex) {
      if (isInSplit(pSplitIndex) && inSplit.length > 1) {
        Set<Integer> newSplit = Sets.newHashSetWithExpectedSize(inSplit.length);
        for (int splitIndex : inSplit) {
          newSplit.add(splitIndex);
        }
        newSplit.remove(pSplitIndex);
        return new SetSplitInfoState(newSplit);
      }

      return this;
    }

    @Override
    public boolean isOneElementalSplit() {
      return inSplit.length == 1;
    }

    @Override
    public SplitInfoState removeFromSplit(final Collection<Integer> pRemoveIndices) {
      Preconditions.checkNotNull(pRemoveIndices);

      Set<Integer> newSplit =
          Sets.newHashSetWithExpectedSize(Math.max(0, inSplit.length - pRemoveIndices.size()));
      for (int splitIndex : inSplit) {
        if (!pRemoveIndices.contains(splitIndex)) {
          newSplit.add(splitIndex);
        }
      }

      if (newSplit.size() < 1) {
        return this;
      }

      return new SetSplitInfoState(newSplit);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(inSplit);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SequenceSplitInfoState other = (SequenceSplitInfoState) obj;
      if (!Arrays.equals(inSplit, other.inSplit)) {
        return false;
      }
      return true;
    }
  }

  static final class SetSplitInfoState extends SplitInfoState {
    private final Set<Integer> inSplit;

    private SetSplitInfoState(final Set<Integer> pInSplit) {
      inSplit = pInSplit;
    }

    @Override
    public SplitInfoState getSplitPart(final int pNumSplitParts, final int pSplitPart) {
      Preconditions.checkArgument(
          pNumSplitParts > 0, "Number of split parts must be a positive number.");
      Preconditions.checkArgument(
          pSplitPart >= 0 && pSplitPart < pNumSplitParts,
          "The split part must be in [0,numSplitParts).");

      if(inSplit.size() < pNumSplitParts) {
        return this;
      }

      ImmutableList<Integer> arr = ImmutableList.sortedCopyOf(inSplit);
      int minElem = inSplit.size() / pNumSplitParts;
      int numAdditionalElem = inSplit.size() % pNumSplitParts;

      Set<Integer> newSplit = Sets.newHashSetWithExpectedSize(minElem + 1);

      for (int i =
              pSplitPart * minElem
                  + (pSplitPart < numAdditionalElem ? pSplitPart : numAdditionalElem);
          i
              < ((pSplitPart + 1) * minElem
                  + ((pSplitPart + 1) < numAdditionalElem ? pSplitPart + 1 : numAdditionalElem));
          i++) {
        newSplit.add(arr.get(i));
      }

      return new SetSplitInfoState(newSplit);
    }

    @Override
    public boolean isInSplit(int pSplitIndex) {
      return inSplit.contains(pSplitIndex);
    }

    @Override
    public SplitInfoState removeFromSplit(int pSplitIndex) {
      if (isInSplit(pSplitIndex) && inSplit.size() > 1) {
        Set<Integer> newSplit = new HashSet<>(inSplit);
        newSplit.remove(pSplitIndex);
        return new SetSplitInfoState(newSplit);
      }
      return this;
    }

    @Override
    public SplitInfoState removeFromSplit(Collection<Integer> pRemoveIndices) {
      Preconditions.checkNotNull(pRemoveIndices);

      Set<Integer> newSplit = new HashSet<>(inSplit);
      newSplit.removeAll(pRemoveIndices);

      if (newSplit.size() < 1) {
        return this;
      }

      return new SetSplitInfoState(newSplit);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((inSplit == null) ? 0 : inSplit.hashCode());
      return result;
    }

    @Override
    public boolean isOneElementalSplit() {
      return inSplit.size() == 1;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SetSplitInfoState other = (SetSplitInfoState) obj;
      if (inSplit == null) {
        if (other.inSplit != null) {
          return false;
        }
      } else if (!inSplit.equals(other.inSplit)) {
        return false;
      }
      return true;
    }
  }
}
