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

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;

/**
 * Utility class to visit all wrapped abstract elements
 * (including the wrapper elements)
 */
public abstract class AbstractWrappedElementVisitor {

  /**
   * Operation to apply on an element when it is visited
   */
  protected abstract void process(AbstractElement element);

  /**
   * Visit a given abstract element and all its sub-elements
   */
  public final void visit(AbstractElement element) {
    process(element);

    if (element instanceof AbstractWrapperElement) {
      AbstractWrapperElement wrapperElement = (AbstractWrapperElement) element;
      for (AbstractElement wrappedElement : wrapperElement.getWrappedElements()) {
        visit(wrappedElement);
      }
    }
  }

}
