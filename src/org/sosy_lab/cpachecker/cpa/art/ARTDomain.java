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
package org.sosy_lab.cpachecker.cpa.art;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class ARTDomain implements AbstractDomain {

  private final ARTCPA cpa;

  private static class ArtBottomElement extends ARTElement
  {
    public ArtBottomElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof ArtBottomElement);
    }

    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }

    @Override
    public String toString() {
      return "ArtBottomElement";
    }
  }

  private static class ArtTopElement extends ARTElement
  {
    public ArtTopElement() {
      super(null, null);
    }

    @Override
    public boolean equals(Object pOther) {

      if (pOther == null) {
        return false;
      }

      return (pOther instanceof ArtTopElement);
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
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
    {
      if (element1.equals (element2)){
        return true;
      }

      if (element1 instanceof ArtBottomElement || element2 instanceof ArtTopElement){
        return true;
      }

      return false;
    }
  }

  private static class ArtJoinOperator implements JoinOperator
  {
    public AbstractElement join (AbstractElement element1, AbstractElement element2)
    {
      // Useless code, but helps to catch bugs by causing cast exceptions
      ARTElement artElement1 = (ARTElement) element1;
      ARTElement artElement2 = (ARTElement) element2;

      if (artElement1.equals (artElement2))
        return artElement1;

      if (artElement1.equals(bottomElement))
        return artElement2;
      if (artElement2.equals(bottomElement))
        return artElement1;

      return topElement;
    }
  }

  private final static ArtBottomElement bottomElement = new ArtBottomElement ();
  private final static ArtTopElement topElement = new ArtTopElement ();
  private final static PartialOrder partialOrder = new ArtPartialOrder ();
  private final static JoinOperator joinOperator = new ArtJoinOperator ();

  public ARTDomain(ARTCPA pCpa)
  {
    cpa = pCpa;
  }

  public AbstractElement getBottomElement ()
  {
      return bottomElement;
  }

  public AbstractElement getTopElement ()
  {
      return topElement;
  }

  public JoinOperator getJoinOperator ()
  {
      return joinOperator;
  }

  public PartialOrder getPartialOrder ()
  {
      return partialOrder;
  }

  public ARTCPA getCpa(){
    return cpa;
  }

}