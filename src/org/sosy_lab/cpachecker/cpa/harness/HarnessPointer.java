/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.harness;

import com.google.common.base.Optional;

public class HarnessPointer {

  private String identifier;

  public HarnessPointer HarnessPointer(String pointerName) {
    identifer = pointerName;
    pointerPosition = new PointerPosition(false,false);
    definingFunction = empty;
    return this;
}

public Pointer(String pointerName, String definingFunctionName){
    DefiningFunction definingFunction = new DefiningFunction(definingFunctionName);
    PointerPosition pointerPosition;
    if (definingFunction.isExtFunction) {
      pointerPosition = new PointerPosition(true, false);
    } else {
      pointerPosition = new PointerPosition(false,false);
    }
    return this
}

  private PointerPosition position;

  public PointerPosition getPosition() {
    return position;
  }

  private DefiningFunction definedBy;

  public DefiningFunction getDefinedBy() {
    return definedBy;
}

public void assumeAlias(Optional<HarnessPointer> pointerToAlias) {
    if (pointerToAlias.isEmpty()) {
      return;
    }
    if (position.isKnown && pointerToAlias.isKnown) {
        if(position.isSpecific && !pointerToAlias.isSpecific) {
             pointerToAlias.position = position;
        } else if(!position.isSpecific && pointerToALias.isSpecific) {
            position = pointerToAlias.position;
        }
        if(isPositionalAlias() && pointerToAlias.isPositionalAlias()) {
            aliasTarget.pointedFrom().newPositionalAlias(pointerToAlias.positionalAliasTarget);
        } else if(isPositionalAlias && !pointerToAlias.isPositionalAlias()
    if bothKnown & exactly one specific update unspecific ones index to the specific ones, update its extcall;
    }

  }

}
