/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

/**
 * Live view of an unmodifiable reached element set, where elements
 * and precision are transformed by mapping functions.
 */
public class UnmodifiableReachedSetView
  implements UnmodifiableReachedSet
{
  private final UnmodifiableReachedSet underlying;
  private final Function<? super AbstractElement, AbstractElement> mapElementFunction;
  private final Function<? super Precision, Precision> mapPrecisionFunction;
  private final Function<Pair<AbstractElement, Precision>, Pair<AbstractElement, Precision>> mapElementAndPrecisionFunction;

  public UnmodifiableReachedSetView(
      UnmodifiableReachedSet pUnderlyingSet,
      Function<? super AbstractElement, AbstractElement> pMapElementFunction,
      Function<? super Precision, Precision> pMapPrecisionFunction) {
    assert pUnderlyingSet != null;
    assert pMapElementFunction != null;
    assert pMapPrecisionFunction != null;

    underlying = pUnderlyingSet;
    mapElementFunction = pMapElementFunction;
    mapPrecisionFunction = pMapPrecisionFunction;
    mapElementAndPrecisionFunction =
      new Function<Pair<AbstractElement,Precision>, Pair<AbstractElement,Precision>>() {
        @Override
        public Pair<AbstractElement, Precision> apply(Pair<AbstractElement, Precision> from) {
          return Pair.of(
              mapElementFunction.apply(from.getFirst()),
              mapPrecisionFunction.apply(from.getSecond()));
        }
      };
  }

  @Override
  public AbstractElement getFirstElement() {
    return mapElementFunction.apply(underlying.getFirstElement());
  }

  @Override
  public AbstractElement getLastElement() {
    return mapElementFunction.apply(underlying.getLastElement());
  }

  @Override
  public Precision getPrecision(AbstractElement pElement) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping prevents reverse mapping");
  }

  @Override
  public Collection<AbstractElement> getReached() {
    return Collections2.transform(underlying.getReached(), mapElementFunction);
  }

  @Override
  public Collection<AbstractElement> getReached(AbstractElement pElement) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Unwrapping may prevent to know the location");
  }

  @Override
  public Collection<AbstractElement> getReached(CFANode pLocation) {
    return Collections2.transform(underlying.getReached(pLocation), mapElementFunction);
  }

  @Override
  public Collection<Pair<AbstractElement, Precision>> getReachedWithPrecision() {
    return Collections2.transform(underlying.getReachedWithPrecision(), mapElementAndPrecisionFunction);
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections2.transform(underlying.getPrecisions(), mapPrecisionFunction);
  }

  @Override
  public Collection<AbstractElement> getWaitlist() {
    return Collections2.transform(underlying.getWaitlist(), mapElementFunction);
  }

  @Override
  public int getWaitlistSize() {
    return underlying.getWaitlistSize();
  }

  @Override
  public boolean hasWaitingElement() {
    return underlying.hasWaitingElement();
  }

  @Override
  public Iterator<AbstractElement> iterator() {
    return Iterators.transform(underlying.iterator(), mapElementFunction);
  }

  @Override
  public boolean contains(AbstractElement pElement) {
    throw new UnsupportedOperationException("Unwrapping may prevent to check contains");
  }

  @Override
  public boolean isEmpty() {
    return underlying.isEmpty();
  }

  @Override
  public int size() {
    return underlying.size();
  }

}
