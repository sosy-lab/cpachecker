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
/**
 *
 */
package org.sosy_lab.cpachecker.cpa.pointsto;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class PointsToDomain implements AbstractDomain {

  private static class PointsToTopElement extends PointsToElement {}

  private static class PointsToBottomElement extends PointsToElement {}

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

  private final static PointsToBottomElement bottomElement = new PointsToBottomElement ();
  private final static PointsToTopElement topElement = new PointsToTopElement ();
  private final static PartialOrder partialOrder = new PointsToPartialOrder ();
  private final static JoinOperator joinOperator = new PointsToJoinOperator ();

  public PointsToDomain () {	}

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getBottomElement()
   */
  public AbstractElement getBottomElement() {
    return bottomElement;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getJoinOperator()
   */
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getPreOrder()
   */
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.AbstractDomain#getTopElement()
   */
  public AbstractElement getTopElement() {
    return topElement;
  }

}
