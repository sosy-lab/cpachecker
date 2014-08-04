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
  public Precision replaceWrappedPrecision(Precision newPrecision, Class<? extends Precision> replaceType) {
    assert replaceType.isAssignableFrom(newPrecision.getClass());

    if (replaceType.equals(CompositePrecision.class)) {
      return newPrecision;
    }

    ImmutableList.Builder<Precision> newPrecisions = ImmutableList.builder();
    boolean changed = false;
    for (Precision precision : precisions) {
      if (replaceType.isAssignableFrom(precision.getClass())) {
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
}
