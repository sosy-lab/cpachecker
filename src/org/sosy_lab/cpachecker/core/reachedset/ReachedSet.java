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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Interface representing a set of reached elements, including storing a
 * precision for each one.
 *
 * In all its operations it preserves the order in which the elements were added.
 * All the collections returned from methods of this class ensure this ordering, too.
 *
 * Classes implementing this interface may not allow null values for elements and precisions.
 * All methods do not return null except when stated explicitly.
 */
public interface ReachedSet extends UnmodifiableReachedSet {

  /**
   * Add an element with a precision to the reached set and to the waitlist.
   * If the element is already in the reached set and the precisions are equal,
   * nothing is done.
   *
   * @param element An AbstractElement.
   * @param precision The Precision for the AbstractElement
   * @throws IllegalArgumentException If the element is already in the reached set, but with a different precision.
   */
  public void add(AbstractElement element, Precision precision) throws IllegalArgumentException;


  public void addAll(Iterable<Pair<AbstractElement, Precision>> toAdd);

  /**
   * Re-add an element to the waitlist which is already contained in the reached set.
   */
  public void reAddToWaitlist(AbstractElement e);

  /**
   * Change the precision of an element that is already in the reached set.
   */
  public void updatePrecision(AbstractElement e, Precision newPrecision);

  public void remove(AbstractElement element);

  public void removeAll(Iterable<? extends AbstractElement> toRemove);

  public void removeOnlyFromWaitlist(AbstractElement element);

  public void clear();

  public AbstractElement popFromWaitlist();
}
