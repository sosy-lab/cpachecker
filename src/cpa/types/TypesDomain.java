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
package cpa.types;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;

/**
 * @author Philipp Wendler
 */
public class TypesDomain implements AbstractDomain {

  private static class TypesBottomElement extends TypesElement {
    @Override
    public String toString() {
      return "<Types BOTTOM>";
    }
  }
  
  private static class TypesTopElement extends TypesElement {
    @Override
    public String toString() {
      return "<Types TOP>";
    }
  }
  
  private static class TypesJoinOperator implements JoinOperator {
    @Override
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      
      TypesElement typesElement1 = (TypesElement)element1;
      TypesElement typesElement2 = (TypesElement)element2;
      
      TypesElement typesElementNew = typesElement1.clone();
      
      typesElementNew.join(typesElement2);
      return typesElementNew;
    }
  }
  
  private static class TypesPartialOrder implements PartialOrder {
    @Override
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2)
                                         throws CPAException {
      
      if (element1 == bottomElement || element2 == topElement) {
        return true;
      }
      if (element2 == bottomElement || element1 == topElement) {
        return false;
      }
      
      return (element1 == element2) || ((TypesElement)element1).isSubsetOf(((TypesElement)element2));
    }
  }
  
  private static final JoinOperator joinOperator = new TypesJoinOperator();
  private static final PartialOrder partialOrder = new TypesPartialOrder();
  private static final AbstractElement bottomElement = new TypesBottomElement();
  private static final AbstractElement topElement = new TypesTopElement();
  
  @Override
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  @Override
  public AbstractElement getBottomElement() {
    return bottomElement;
  }

  @Override
  public AbstractElement getTopElement() {
    return topElement;
  }
}