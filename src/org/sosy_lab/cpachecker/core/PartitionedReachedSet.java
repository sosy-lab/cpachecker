/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * Special implementation of the reached set that needs abstract elements which
 * implement {@link Partitionable}.
 *
 * Whenever the method {@link PartitionedReachedSet#getReached(AbstractElement)}
 * is called (which is usually done by the CPAAlgorithm to get the candidates
 * for merging and coverage checks), it will return a subset of the set of all
 * reached elements. This subset contains exactly those elements, whose partition
 * key is equal to the key of the element given as a parameter.
 */
public class PartitionedReachedSet extends ReachedElements {

  private final SetMultimap<Object, AbstractElement> partitionedReached = LinkedHashMultimap.create();
  
  public PartitionedReachedSet(TraversalMethod traversal) {
    super(traversal);
  }
  
  @Override
  public void add(AbstractElement pElement, Precision pPrecision) {
    super.add(pElement, pPrecision);
    
    assert pElement instanceof Partitionable : "Partitionable elements necessary for PartitionedReachedSet";
    Object key = ((Partitionable)pElement).getPartitionKey();
    partitionedReached.put(key, pElement);
  }
  
  @Override
  public void remove(AbstractElement pElement) {
    super.remove(pElement);
    
    assert pElement instanceof Partitionable : "Partitionable elements necessary for PartitionedReachedSet";
    Object key = ((Partitionable)pElement).getPartitionKey();
    partitionedReached.remove(key, pElement);
  }
  
  @Override
  public void clear() {
    super.clear();
    
    partitionedReached.clear();
  }
  
  @Override
  public Set<AbstractElement> getReached(AbstractElement pElement) {
    assert pElement instanceof Partitionable : "Partitionable elements necessary for PartitionedReachedSet";
    Object key = ((Partitionable)pElement).getPartitionKey();
    return Collections.unmodifiableSet(partitionedReached.get(key));
  }
}
