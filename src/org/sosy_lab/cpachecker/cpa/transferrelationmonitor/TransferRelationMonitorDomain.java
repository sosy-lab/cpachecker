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
package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class TransferRelationMonitorDomain implements AbstractDomain{

  private final TransferRelationMonitorCPA cpa;

  private static class TransferRelationMonitorBottomElement extends TransferRelationMonitorElement
  {
    public TransferRelationMonitorBottomElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof TransferRelationMonitorBottomElement);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public String toString() {
      return "TransferRelationMonitorBottomElement";
    }
  }

  private static class TransferRelationMonitorTopElement extends TransferRelationMonitorElement
  {
    public TransferRelationMonitorTopElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof TransferRelationMonitorTopElement);
    }

    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
      return "ArtTopElement";
    }
  }

  private static class ArtPartialOrder implements PartialOrder
  {
    @Override
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
    {
      if (element1.equals (element2)){
        return true;
      }

      if (element1 instanceof TransferRelationMonitorBottomElement || element2 instanceof TransferRelationMonitorTopElement){
        return true;
      }

      return false;
    }
  }

  private static class ArtJoinOperator implements JoinOperator
  {
    @Override
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      // Useless code, but helps to catch bugs by causing cast exceptions
      TransferRelationMonitorElement transferRelationMonitorElement1 = (TransferRelationMonitorElement) element1;
      TransferRelationMonitorElement transferRelationMonitorElement2 = (TransferRelationMonitorElement) element2;

      if (transferRelationMonitorElement1.equals (transferRelationMonitorElement2))
        return transferRelationMonitorElement1;

      if (transferRelationMonitorElement1.equals(bottomElement))
        return transferRelationMonitorElement2;
      if (transferRelationMonitorElement2.equals(bottomElement))
        return transferRelationMonitorElement1;

      return topElement;
    }
  }

  private final static TransferRelationMonitorBottomElement bottomElement = new TransferRelationMonitorBottomElement ();
  private final static TransferRelationMonitorTopElement topElement = new TransferRelationMonitorTopElement ();
  private final static PartialOrder partialOrder = new ArtPartialOrder ();
  private final static JoinOperator joinOperator = new ArtJoinOperator ();

  public TransferRelationMonitorDomain(TransferRelationMonitorCPA pCpa)
  {
    cpa = pCpa;
  }

  @Override
  public AbstractElement getBottomElement ()
  {
      return bottomElement;
  }

  @Override
  public AbstractElement getTopElement ()
  {
      return topElement;
  }

  @Override
  public JoinOperator getJoinOperator ()
  {
      return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder ()
  {
      return partialOrder;
  }

  public TransferRelationMonitorCPA getCpa(){
    return cpa;
  }
}
