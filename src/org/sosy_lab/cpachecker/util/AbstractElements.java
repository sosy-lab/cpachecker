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
package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Helper class that provides several useful methods for handling AbstractElements.
 */
public final class AbstractElements {

  private AbstractElements() { }
  
  public static <T extends AbstractElement> T extractElementByType(AbstractElement pElement, Class<T> pType) {
    if (pType.isInstance(pElement)) {
      return pType.cast(pElement);
    } else if (pElement instanceof AbstractWrapperElement) {
      return ((AbstractWrapperElement)pElement).retrieveWrappedElement(pType);
    } else {
      return null;
    }
  }
  
  public static CFANode extractLocation(AbstractElement pElement) {
    AbstractElementWithLocation e = extractElementByType(pElement, AbstractElementWithLocation.class);
    return e == null ? null : e.getLocationNode();
  }
  
  public static boolean isTargetElement(AbstractElement e) {
    return (e instanceof Targetable) && ((Targetable)e).isTarget();
  }
  
  public static Predicate<AbstractElement> FILTER_TARGET_ELEMENTS = new Predicate<AbstractElement>() {
    @Override
    public boolean apply(AbstractElement pArg0) {
      return isTargetElement(pArg0);
    }
  };
  
  public static <T extends AbstractElement>
                Function<AbstractElement, T> extractElementByTypeFunction(final Class<T> pType) {
    
    return new Function<AbstractElement, T>() {
      @Override
      public T apply(AbstractElement ae) {
        return extractElementByType(ae, pType);
      }
    };
  }
}
