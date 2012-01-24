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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements a domain for CPAs, where the partial order is
 * identical to the equality relation, if both of the two operands are neither
 * bottom nor top. The resulting lattice is a layered graph with three layers
 * (one for top, one for bottom and one for all other elements) and edges only
 * between different layers.
 */
public class FlatLatticeDomain implements AbstractDomain {
  private final AbstractElement mTopElement;

  private static class TopElement implements AbstractElement {
    @Override
    public String toString() {
      return "<TOP>";
    }
  }

  public FlatLatticeDomain(AbstractElement pTopElement) {
    assert(pTopElement != null);

    this.mTopElement = pTopElement;
  }

  public FlatLatticeDomain() {
    this(new TopElement());
  }

  @Override
  public AbstractElement join(AbstractElement pElement1, AbstractElement pElement2) throws CPAException {
    if (isLessOrEqual(pElement1, pElement2)) {
      return pElement2;
    }

    if (isLessOrEqual(pElement2, pElement1)) {
      return pElement1;
    }

    return mTopElement;
  }

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) throws CPAException {
    return (mTopElement.equals(reachedElement) || newElement.equals(reachedElement));
  }
}
