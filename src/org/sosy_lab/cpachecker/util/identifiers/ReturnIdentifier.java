/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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


public class ReturnIdentifier extends VariableIdentifier implements GeneralIdentifier {

  private static ReturnIdentifier instance;

  private ReturnIdentifier(String pNm, CType pTp, int pDereference) {
    super(pNm, pTp, pDereference);
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public SingleIdentifier clone() {
    return instance;
  }

  @Override
  public SingleIdentifier clearDereference() {
    return instance;
  }

  @Override
  public String toLog() {
    return "r";
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return null;
  }

  public static ReturnIdentifier getInstance() {
    if (instance == null) {
      instance = new ReturnIdentifier("__returnId", null, 0);
    }
    return instance;
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO == instance) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public DataType getType(Map<? extends AbstractIdentifier, DataType> pLocalInfo) {
    return pLocalInfo.get(instance);
  }

}
