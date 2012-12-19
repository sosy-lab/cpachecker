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
package org.sosy_lab.cpachecker.cpa.cpalien;


public class SMGObject {
  final private int size_in_bytes;
  final private String label;
  final private boolean nullObject;

  public SMGObject(int pSize_in_bytes, String pLabel) {
    super();
    size_in_bytes = pSize_in_bytes;
    label = pLabel;
    nullObject = false;
  }

  public SMGObject(){
    super();
    size_in_bytes = 0;
    label = "NULL";
    nullObject = true;
  }

  public String getLabel() {
    return label;
  }

  public int getSizeInBytes() {
    return size_in_bytes;
  }

  @Override
  public String toString() {
    return label + "(" + size_in_bytes + "b)";
  }

  public boolean notNull() {
    return !nullObject;
  }
}