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
package org.sosy_lab.cpachecker.cpa.pointer;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

public class PointerDomain implements AbstractDomain {

  @Override
  public AbstractElement join(AbstractElement element1, AbstractElement element2) {
    throw new UnsupportedOperationException();
    /*
      PointerElement pointerElement1 = (PointerElement)element1;
      PointerElement pointerElement2 = (PointerElement)element2;

      PointerElement pointerElementNew = pointerElement1.clone();

      // TODO Join Operator
      Map<String, Pointer> pointers2   = pointerElement2.getGlobalPointers();
      Map<String, Pointer> pointersNew = pointerElementNew.getGlobalPointers();
      for (String name : pointers2.keySet()) {
        if (pointersNew.containsKey(name)) {
          pointersNew.get(name).join(pointers2.get(name));
        } else {
          pointersNew.put(name, pointers2.get(name).clone());
        }
      }

      pointers2   = pointerElement2.getLocalPointers();
      pointersNew = pointerElementNew.getLocalPointers();
      for (String name : pointers2.keySet()) {
        if (pointersNew.containsKey(name)) {
          pointersNew.get(name).join(pointers2.get(name));
        } else {
          pointersNew.put(name, pointers2.get(name).clone());
        }
      }
      // only the local variables of the current context need to be joined,
      // the others are already identical (were joined before calling the last function)
      return pointerElementNew;
    */
  }

  @Override
  public boolean isLessOrEqual(AbstractElement newElement, AbstractElement reachedElement) {
      PointerElement newPointerElement = (PointerElement)newElement;
      PointerElement reachedPointerElement = (PointerElement)reachedElement;

      return reachedPointerElement.isSubsetOf(newPointerElement);
  }
}
