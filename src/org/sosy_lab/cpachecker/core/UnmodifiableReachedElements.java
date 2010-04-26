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

import java.util.Collection;
import java.util.Iterator;

import javax.naming.OperationNotSupportedException;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.ReachedElements.TraversalMethod;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Interface representing an unmodifiable reached set
 * 
 * @author g.theoduloz
 */
public interface UnmodifiableReachedElements extends Iterable<AbstractElement> {

  public Collection<AbstractElement> getReached();

  @Override
  public Iterator<AbstractElement> iterator();

  public Collection<Pair<AbstractElement, Precision>> getReachedWithPrecision();

  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * elements belonging to the same location as a given element. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states. 
   * 
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   * 
   * The returned set is unmodifiable.
   * 
   * @param element An abstract element for whose location the abstract states should be retrieved.
   * @return A subset of the reached set.
   */
  public Collection<AbstractElement> getReached(AbstractElement element)
    throws OperationNotSupportedException;
  
  /**
   * Returns a subset of the reached set, which contains at least all abstract
   * elements belonging to given location. It may even
   * return an empty set if there are no such states. Note that it may return up to
   * all abstract states. 
   * 
   * The returned set is a view of the actual data, so it might change if nodes
   * are added to the reached set. Subsequent calls to this method with the same
   * parameter value will always return the same object.
   * 
   * The returned set is unmodifiable.
   * 
   * @param location A location
   * @return A subset of the reached set.
   */
  public Collection<AbstractElement> getReached(CFANode location);

  public AbstractElement getFirstElement();

  public AbstractElement getLastElement();

  public TraversalMethod getTraversalMethod();

  public boolean hasWaitingElement();

  public Collection<AbstractElement> getWaitlist();

  public int getWaitlistSize();

  /**
   * Returns the precision for an element.
   * @param element The element to look for.
   * @return The precision for the element or null.
   */
  public Precision getPrecision(AbstractElement element)
    throws OperationNotSupportedException;

  public int size();

  public String toString();

}