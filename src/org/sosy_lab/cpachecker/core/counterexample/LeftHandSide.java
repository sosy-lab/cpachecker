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
package org.sosy_lab.cpachecker.core.counterexample;

public abstract class LeftHandSide {

  private final String name;
  private final String functionName;

  public LeftHandSide(String pName, String pFunctionName) {
    name = pName;
    functionName = pFunctionName;
  }

  public LeftHandSide(String pName) {
    name = pName;
    functionName = null;
  }

  public String getName() {
    return name;
  }

  public String getFunctionName() {
    assert functionName != null;
    return functionName;
  }

  public boolean isGlobal() {
    return functionName == null;
  }

  @Override
  public abstract boolean equals(Object pObj);

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    return functionName == null ? name : functionName + "::" + name;
  }
}