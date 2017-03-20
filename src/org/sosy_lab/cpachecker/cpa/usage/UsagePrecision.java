/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;

import com.google.common.base.Predicate;


public class UsagePrecision implements WrapperPrecision {
  private HashMap<CFANode, Map<GeneralIdentifier, DataType>> localStatistics;
  private final Precision wrappedPrecision;

  UsagePrecision(Precision pWrappedPrecision) {
    localStatistics = new HashMap<>();
    wrappedPrecision = pWrappedPrecision;
  }

  public boolean add(CFANode node, Map<GeneralIdentifier, DataType> info) {
    if (!localStatistics.containsKey(node)) {
      localStatistics.put(node, info);
      return true;
    } else {
      //strange situation, we should know about it, because we consider, that nodes in file are unique
      return false;
    }
  }

  public Map<GeneralIdentifier, DataType> get(CFANode node) {
    return localStatistics.get(node);
  }

  public Precision getWrappedPrecision() {
    return wrappedPrecision;
  }

  public UsagePrecision clone(Precision wrappedPrecision) {
    UsagePrecision newPrecision = new UsagePrecision(wrappedPrecision);
    newPrecision.localStatistics = this.localStatistics;
    return newPrecision;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((localStatistics == null) ? 0 : localStatistics.hashCode());
    result = prime * result + ((wrappedPrecision == null) ? 0 : wrappedPrecision.hashCode());
    return result;
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
    UsagePrecision other = (UsagePrecision) obj;
    if (localStatistics == null) {
      if (other.localStatistics != null) {
        return false;
      }
    } else if (!localStatistics.equals(other.localStatistics)) {
      return false;
    }
    if (wrappedPrecision == null) {
      if (other.wrappedPrecision != null) {
        return false;
      }
    } else if (!wrappedPrecision.equals(other.wrappedPrecision)) {
      return false;
    }
    return true;
  }

  @Override
  public UsagePrecision clone() {
    return clone(this.wrappedPrecision);
  }

  @Override
  public String toString() {
    String s = "Size = " + localStatistics.size() + ";";
    s += wrappedPrecision.toString();
    return s;
  }

  public int getTotalRecords() {
    int sum = 0;
    for (CFANode node : localStatistics.keySet()) {
      sum += localStatistics.get(node).size();
    }
    return sum;
  }

  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedPrecision.getClass())) {
      return pType.cast(wrappedPrecision);
    } else if (wrappedPrecision instanceof WrapperPrecision) {
      return ((WrapperPrecision) wrappedPrecision).retrieveWrappedPrecision(pType);
    } else {
      return null;
    }
  }

  @Override
  public Precision replaceWrappedPrecision(Precision pNewPrecision, Predicate<? super Precision> pReplaceType) {
    if (pReplaceType.apply(this)) {
      return pNewPrecision;
    } else if (pReplaceType.apply(wrappedPrecision)) {
      UsagePrecision result = new UsagePrecision(pNewPrecision);
      result.localStatistics = this.localStatistics;
      return result;
    } else if (wrappedPrecision instanceof WrapperPrecision) {
      UsagePrecision result = new UsagePrecision(((WrapperPrecision) wrappedPrecision).replaceWrappedPrecision(pNewPrecision, pReplaceType));
      result.localStatistics = this.localStatistics;
      return result;

    } else {
      return null;
    }
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return Collections.singleton(wrappedPrecision);
  }
}
