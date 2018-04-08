/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.rcucpa;

import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GlobalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureFieldIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class LocationIdentifierConverter {
  public static AbstractIdentifier toId(MemoryLocation location) {
    return null;
  }

  public static MemoryLocation toLocation(AbstractIdentifier id) {
    MemoryLocation result = null;

    if (id instanceof LocalVariableIdentifier) {
      LocalVariableIdentifier lvid = (LocalVariableIdentifier) id;
      result = MemoryLocation.valueOf(lvid.getFunction(), lvid.getName());
    } else if (id instanceof GlobalVariableIdentifier) {
      GlobalVariableIdentifier gvid = (GlobalVariableIdentifier) id;
      result = MemoryLocation.valueOf(gvid.getName());
    } else if (id instanceof StructureFieldIdentifier) {
      StructureFieldIdentifier stfid = (StructureFieldIdentifier) id;
      AbstractIdentifier owner = stfid.getOwner();
      if (owner != null) {
        String ownerType = ((SingleIdentifier)owner).getType().toString();
        if (ownerType.contains("(")) {
          ownerType = ownerType.substring(ownerType.indexOf("(") + 1, ownerType.indexOf(")"));
        }
        return MemoryLocation.valueOf(ownerType + "." +
            ((SingleIdentifier)stfid).getName());
      } else {
        return MemoryLocation.valueOf(stfid.toString());
      }
    } else if (id instanceof StructureIdentifier) {
      StructureIdentifier stid = (StructureIdentifier) id;
      AbstractIdentifier owner = stid.getOwner();
      if (owner != null) {
        String ownerType = ((SingleIdentifier)owner).getType().toString();
        if (ownerType.contains("(")) {
          ownerType = ownerType.substring(ownerType.indexOf("(") + 1, ownerType.indexOf(")"));
        }
        return MemoryLocation.valueOf(ownerType + "." +
            ((SingleIdentifier)stid).getName());
      } else {
        return MemoryLocation.valueOf(stid.toString());
      }
    }

    return result;
  }
}
