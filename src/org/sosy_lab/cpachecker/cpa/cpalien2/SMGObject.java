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
package org.sosy_lab.cpachecker.cpa.cpalien2;


public class SMGObject {
  final private int size_in_bytes;
  final private String label;
  final private boolean nullObject;

  public SMGObject(int pSize_in_bytes, String pLabel) {
    size_in_bytes = pSize_in_bytes;
    label = pLabel;
    nullObject = false;
  }

  public SMGObject() {
    size_in_bytes = 0;
    label = "NULL";
    nullObject = true;
  }

  public SMGObject(SMGObject pObject) {
    size_in_bytes = pObject.size_in_bytes;
    label = pObject.label;
    nullObject = pObject.nullObject;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public boolean equals(Object pObj) {
    // Note that Objects are not
    // equal because there fields are equal.
    // Objects can belong to different StackFrames.
    return super.equals(pObj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
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

  public boolean propertiesEqual(SMGObject other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }

    if (label == null) {
      if (other.label != null) {
        return false;
      }
    } else if (!label.equals(other.label)) {
      return false;
    }
    if (nullObject != other.nullObject) {
      return false;
    }
    if (size_in_bytes != other.size_in_bytes) {
      return false;
    }
    return true;
  }
}