/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface AbstractDomain {

  /**
   * Returns the smallest element of the lattice that is greater than both
   * elements (the join).
   *
   * This is an optional method. If a domain is expected to be used only with
   * merge-sep, it does not have to provide an implementation of this method.
   * This method should then throw an {@link UnsupportedOperationException}.
   *
   * @param element1 an abstract element
   * @param element2 an abstract element
   * @return the join of element1 and element2
   * @throws CPAException If any error occurred.
   * @throws UnsupportedOperationException If this domain does not provide a join method.
   */
  public AbstractElement join(AbstractElement element1, AbstractElement element2) throws CPAException;

  /**
   * Returns true if element1 is less or equal than element with respect to
   * the lattice.
   *
   * @param element1 an abstract element
   * @param element2 an abstract element
   * @return (element1 <= element2)
   * @throws CPAException If any error occurred.
   */
  public boolean isLessOrEqual(AbstractElement element1, AbstractElement element2) throws CPAException;

}
