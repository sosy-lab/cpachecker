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
/**
 *
 */
package cpa.pointsto;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToDomain implements AbstractDomain {

  private static class PointsToTopElement extends PointsToElement implements TopElement {}

  private static class PointsToBottomElement extends PointsToElement implements BottomElement {}

  private static class PointsToPartialOrder implements PartialOrder {
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) {
      PointsToElement pointsToElement1 = (PointsToElement) element1;
      PointsToElement pointsToElement2 = (PointsToElement) element2;

      if (pointsToElement2.equals(topElement)) return true;
      if (pointsToElement1.equals(bottomElement)) return true;
      return pointsToElement1.subsetOf(pointsToElement2);
    }
  }

  private static class PointsToJoinOperator implements JoinOperator
  {
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      PointsToElement joined = ((PointsToElement) element1).clone();
      joined.join((PointsToElement) element2);
      return joined;
    }
  }

  private final static BottomElement bottomElement = new PointsToBottomElement ();
  private final static TopElement topElement = new PointsToTopElement ();
  private final static PartialOrder partialOrder = new PointsToPartialOrder ();
  private final static JoinOperator joinOperator = new PointsToJoinOperator ();

  public PointsToDomain () {	}

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getBottomElement()
   */
  public AbstractElement getBottomElement() {
    return bottomElement;
  }

  public boolean isBottomElement(AbstractElement element) {
    return element.equals(bottomElement);
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getJoinOperator()
   */
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getPreOrder()
   */
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  /* (non-Javadoc)
   * @see cpa.common.interfaces.AbstractDomain#getTopElement()
   */
  public AbstractElement getTopElement() {
    return topElement;
  }

}
