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
package org.sosy_lab.cpachecker.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class DummyUnmodifiableReachedSet implements UnmodifiableReachedSet {

  private final Collection<AbstractState> reached;

  public DummyUnmodifiableReachedSet(Collection<AbstractState> pReached) {
    reached = pReached;
  }

  @Override
  public Collection<AbstractState> asCollection() {
    return reached;
  }

  @Override
  public Iterator<AbstractState> iterator() {
    return reached.iterator();
  }

  @Override
  public Collection<Precision> getPrecisions() {
    return Collections.emptySet();
  }

  @Override
  public Collection<AbstractState> getReached(AbstractState pState)
      throws UnsupportedOperationException {
    return reached;
  }

  @Override
  public Collection<AbstractState> getReached(CFANode pLocation) {
    return reached;
  }

  @Override
  public AbstractState getFirstState() {
    return reached.iterator().next();
  }

  @Override
  public @Nullable AbstractState getLastState() {
    return reached.iterator().next();
  }

  @Override
  public boolean hasWaitingState() {
    return false;
  }

  @Override
  public Collection<AbstractState> getWaitlist() {
    return Collections.emptySet();
  }

  @Override
  public Precision getPrecision(AbstractState pState) throws UnsupportedOperationException {
    return SingletonPrecision.getInstance();
  }

  @Override
  public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(AbstractState pState) {
    return reached.contains(pState);
  }

  @Override
  public boolean isEmpty() {
    return reached.isEmpty();
  }

  @Override
  public int size() {
    return reached.size();
  }

  @Override
  public boolean hasViolatedProperties() {
    return false;
  }

  @Override
  public Collection<Property> getViolatedProperties() {
    return Collections.emptySet();
  }
}
