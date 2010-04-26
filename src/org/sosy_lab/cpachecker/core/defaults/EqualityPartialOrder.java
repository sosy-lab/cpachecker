/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

/**
 * This class implements a partial order for CPAs, where the partial order is
 * identical to the equality relation, if both of the two operands are neither
 * bottom nor top. The resulting lattice is a layered graph with three layers
 * (one for top, one for bottom and one for all other elements) and edges only
 * between different layers. 
 * 
 * @author wendler
 */
public class EqualityPartialOrder implements PartialOrder {

  private final AbstractDomain domain;
  
  public EqualityPartialOrder(AbstractDomain domain) {
    this.domain = domain;
  }
 
  @Override
  public boolean satisfiesPartialOrder(AbstractElement newElement,
                                       AbstractElement reachedElement) {
   
    return(domain.getBottomElement().equals(newElement)
        || domain.getTopElement().equals(reachedElement)
        || newElement.equals(reachedElement));
  }
}