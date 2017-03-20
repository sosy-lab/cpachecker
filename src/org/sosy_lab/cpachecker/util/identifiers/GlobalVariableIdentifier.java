/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;



public class GlobalVariableIdentifier extends VariableIdentifier {

  public GlobalVariableIdentifier(String nm, CType t, int dereference) {
    super(nm, t, dereference);
  }

  @Override
  public GlobalVariableIdentifier clone() {
    return new GlobalVariableIdentifier(name, type, dereference);
  }

  @Override
  public SingleIdentifier clearDereference() {
    return new GlobalVariableIdentifier(name, type, 0);
  }

  @Override
  public boolean isGlobal() {
    return true;
  }

  @Override
  public String toLog() {
    return "g;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralGlobalVariableIdentifier(name, type, dereference);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof GlobalVariableIdentifier) {
      return super.compareTo(pO);
    } else {
      return 1;
    }
  }

  @Override
  public DataType getType(Map<? extends AbstractIdentifier, DataType> pLocalInfo) {
    DataType result = super.getType(pLocalInfo);
    if (result != null) {
      return result;
    }
    return DataType.GLOBAL;
  }
}
