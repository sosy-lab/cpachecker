/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;

public class Precisions {

  private Precisions() { }

  /**
   * Retrieve one of the wrapped precisions by type. If the hierarchy of
   * (wrapped) precisions has several levels, this method searches through
   * them recursively.
   *
   * The type does not need to match exactly, the returned element has just to
   * be a sub-type of the type passed as argument.
   *
   * @param <T> The type of the wrapped element.
   * @param An abstract state
   * @param pType The class object of the type of the wrapped element.
   * @return An instance of an element with type T or null if there is none.
   */
  public static <T extends Precision> T extractPrecisionByType(Precision pPrecision, Class<T> pType) {
    if (pType.isInstance(pPrecision)) {
      return pType.cast(pPrecision);

    } else if (pPrecision instanceof WrapperPrecision) {
      return ((WrapperPrecision)pPrecision).retrieveWrappedPrecision(pType);
    }

    return null;
  }

  public static Precision replaceByType(Precision pOldPrecision, Precision pNewPrecision, Class<? extends Precision> type) {
    if (pOldPrecision instanceof WrapperPrecision) {
      return ((WrapperPrecision)pOldPrecision).replaceWrappedPrecision(pNewPrecision, type);
    } else {
      assert type.isAssignableFrom(pOldPrecision.getClass());
      return pNewPrecision;
    }
  }
}
