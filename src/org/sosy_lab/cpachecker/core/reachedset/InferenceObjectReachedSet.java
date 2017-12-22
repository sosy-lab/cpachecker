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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;

public class InferenceObjectReachedSet implements NestedReachedSet<InferenceObject> {

  private final Set<InferenceObject> reached;

  public InferenceObjectReachedSet() {
    reached = new HashSet<>();
  }

  @Override
  public Collection<InferenceObject> asCollection() {
    return Collections.unmodifiableSet(reached);
  }

  @Override
  public Iterator<InferenceObject> iterator() {
    return asCollection().iterator();
  }

  @Override
  public Collection<InferenceObject> getReached(InferenceObject pState) throws UnsupportedOperationException {
    return asCollection();
  }

  @Override
  public Collection<InferenceObject> getReached(CFANode pLocation) {
    return asCollection();
  }

  @Override
  public InferenceObject getFirstState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public @Nullable InferenceObject getLastState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean contains(InferenceObject pState) {
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
  public void clear() {
    reached.clear();
  }

  public void add(InferenceObject pState) {
    if (pState != EmptyInferenceObject.getInstance() &&
        pState != TauInferenceObject.getInstance()) {
      reached.add(pState);
    }
  }

  public boolean remove(InferenceObject pState) {
    return reached.remove(pState);
  }
}
