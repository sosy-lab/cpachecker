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
package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

public class BAMPrecision implements WrapperPrecision {
  final Precision wrappedPrecision;
  final Set<CFANode> uncachedBlockEntries;

  private BAMPrecision(Set<CFANode> funcs, Precision p) {
    uncachedBlockEntries = funcs;
    wrappedPrecision = p;
  }

  @Override
  public BAMPrecision clone() {
    return new BAMPrecision(Sets.newHashSet(this.uncachedBlockEntries), this.wrappedPrecision);
  }

  public BAMPrecision clone(Precision wrapped) {
    return new BAMPrecision(Sets.newHashSet(this.uncachedBlockEntries), wrapped);
  }

  public BAMPrecision clone(BAMPrecision other, Precision wrapped) {
    return new BAMPrecision(Sets.union(this.uncachedBlockEntries, other.uncachedBlockEntries), wrapped);
  }

  public BAMPrecision(Precision p) {
    uncachedBlockEntries = Sets.newHashSet();
    wrappedPrecision = p;
  }

  public void addUncachedBlock(CFANode node) {
    uncachedBlockEntries.add(node);
  }

  public boolean shouldBeSkipped(CFANode node) {
    return uncachedBlockEntries.contains(node);
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
    } else if (wrappedPrecision instanceof WrapperPrecision) {
      BAMPrecision result = new BAMPrecision(Sets.newHashSet(this.uncachedBlockEntries),
          ((WrapperPrecision) wrappedPrecision).replaceWrappedPrecision(pNewPrecision, pReplaceType));
      return result;

    } else {
      return null;
    }
  }

  public Precision getWrappedPrecision() {
    return wrappedPrecision;
  }

  @Override
  public Iterable<Precision> getWrappedPrecisions() {
    return Collections.singleton(wrappedPrecision);
  }

  @Override
  public String toString() {
    return uncachedBlockEntries.toString();
  }

  public void copyUncachedBlocks(BAMPrecision pExpandedPrecision) {
    uncachedBlockEntries.addAll(pExpandedPrecision.uncachedBlockEntries);
  }
}
