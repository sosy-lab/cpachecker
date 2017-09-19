/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.states;

import java.io.Serializable;

import javax.annotation.Nullable;

public class PointerToMemoryLocation extends MemoryLocation implements Comparable<MemoryLocation>, Serializable {

  private static final long serialVersionUID = -8910967707373729035L;

  private PointerToMemoryLocation(String pFunctionName, String pIdentifier, @Nullable Long pOffset) {
    super(pFunctionName, pIdentifier, pOffset);
  }

  private PointerToMemoryLocation(String pIdentifier, @Nullable Long pOffset) {
    super(pIdentifier, pOffset);
  }

  public static PointerToMemoryLocation valueOf(String pFunctionName, String pIdentifier) {
    if (pFunctionName == null) {
      return new PointerToMemoryLocation(pIdentifier, null);
    } else {
      return new PointerToMemoryLocation(pFunctionName, pIdentifier, null);
    }
  }

  public static PointerToMemoryLocation valueOf(String pFunctionName, String pIdentifier, long pOffset) {
    return new PointerToMemoryLocation(pFunctionName, pIdentifier, pOffset);
  }

  public static PointerToMemoryLocation valueOf(String pVariableName) {

    String[] nameParts    = pVariableName.split("::");
    String[] offsetParts  = pVariableName.split("/");

    boolean isScoped  = nameParts.length == 2;
    boolean hasOffset = offsetParts.length == 2;

    @Nullable Long offset =
        hasOffset ? Long.parseLong(offsetParts[1]) : null;

    if (isScoped) {
      if (hasOffset) {
        nameParts[1] = nameParts[1].replace("/" + offset, "");
      }
      nameParts[1] = "(*" + nameParts[1] + ")";
      return new PointerToMemoryLocation(nameParts[0], nameParts[1], offset);

    } else {
      if (hasOffset) {
        nameParts[0] = nameParts[0].replace("/" + offset, "");
      }
      return new PointerToMemoryLocation(nameParts[0].replace("/" + offset, ""), offset);
    }
  }
}