/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

public class CompositePrecision implements WrapperPrecision {

  private final List<Precision> precisions;

  public CompositePrecision(List<Precision> precisions) {
    this.precisions = ImmutableList.copyOf(precisions);
  }

  public List<Precision> getPrecisions() {
    return precisions;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null || !(other instanceof CompositePrecision)) {
      return false;
    }

    return precisions.equals(((CompositePrecision)other).precisions);
  }

  @Override
  public int hashCode() {
    return precisions.hashCode();
  }

  public Precision get(int idx) {
    return precisions.get(idx);
  }

  @Override
  public String toString() {
    return precisions.toString();
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (Precision precision : precisions) {
      if (pType.isAssignableFrom(precision.getClass())) {
        return pType.cast(precision);

      } else if (precision instanceof WrapperPrecision) {
        T result = ((WrapperPrecision)precision).retrieveWrappedPrecision(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @Override
  public Precision replaceWrappedPrecision(Precision newPrecision, Predicate<? super Precision> replaceType) {

    if (replaceType.apply(this)) {
      return newPrecision;
    }

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();
    boolean changed = false;
    for (Precision precision : precisions) {
      if (replaceType.apply(precision)) {
        newPrecisions.add(newPrecision);
        changed = true;

      } else if (precision instanceof WrapperPrecision) {
        Precision newWrappedPrecision = ((WrapperPrecision)precision).replaceWrappedPrecision(newPrecision, replaceType);
        if (newWrappedPrecision != null) {
          newPrecisions.add(newWrappedPrecision);
          changed = true;

        } else {
          newPrecisions.add(precision);
        }
      } else {
        newPrecisions.add(precision);
      }
    }
    return changed ? new CompositePrecision(newPrecisions.build()) : null;
  }

  @Override
  public Precision replacePrecision(Function<Precision, Precision> pFunction) {

    Precision result = pFunction.apply(this);
    if (result != null && result != this) {
      return result;
    }

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();
    boolean changed = false;

    for (Precision oldWrapped : precisions) {

      final Precision newWrapped;

      if (oldWrapped instanceof WrapperPrecision) {
        newWrapped = ((WrapperPrecision)oldWrapped).replacePrecision(pFunction);
      } else {
        newWrapped = pFunction.apply(oldWrapped);
      }

      if (newWrapped != null && newWrapped != oldWrapped) {
        changed = true;
        newPrecisions.add(newWrapped);

      } else {
        newPrecisions.add(oldWrapped);
      }

    }

    return changed ? new CompositePrecision(newPrecisions.build()) : null;
  }

  @Override
  public int getNumberOfWrapped() {
    return precisions.size();
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return precisions;
  }

  @Override
  public Precision join(Precision pOther) {
    Preconditions.checkArgument(pOther instanceof CompositePrecision);
    CompositePrecision other = (CompositePrecision) pOther;
    Preconditions.checkArgument(other.getNumberOfWrapped() == this.getNumberOfWrapped());

    ImmutableList.Builder<Precision> joinedPrecisions = ImmutableList.builder();
    boolean changed = false;

    for (int index=0; index<precisions.size(); index++) {
      final Precision thisComp = this.precisions.get(index);
      final Precision otherComp = other.precisions.get(index);

      final Precision joinedComp = thisComp.join(otherComp);
      joinedPrecisions.add(joinedComp);

      if (!joinedComp.equals(otherComp)) {
        changed = true;
      }
    }

    return changed ? new CompositePrecision(joinedPrecisions.build()) : null;
  }
}
