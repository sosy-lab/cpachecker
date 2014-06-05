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

import java.util.List;

import com.google.common.collect.ImmutableList;


public final class ReferenceName extends LeftHandSide {

  private final List<String> fieldNames;

  public ReferenceName(String pName, String pFunctionName, List<String> pFieldNames) {
    super(pName, pFunctionName);
    assert pFieldNames.size() > 0;
    fieldNames = ImmutableList.copyOf(pFieldNames);
  }

  public ReferenceName(String pName, List<String> pFieldNames) {
    super(pName);
    assert pFieldNames.size() > 0;
    fieldNames = ImmutableList.copyOf(pFieldNames);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    ReferenceName other = (ReferenceName) obj;

    if (isGlobal()) {
      if (!other.isGlobal()) {
        return false;
      }
    } else if (!getFunctionName().equals(!other.isGlobal() ? other.getFunctionName() : null)) {
      return false;
    }

    if (getName() == null) {
      if (other.getName() != null) {
        return false;
      }
    } else if (!getName().equals(other.getName())) {
      return false;
    }

    if (fieldNames == null) {
      if (other.fieldNames != null) {
        return false;
      }
    } else if (!fieldNames.equals(other.fieldNames)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((isGlobal()) ? 0 : getFunctionName().hashCode());
    result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
    result = prime * result + ((fieldNames == null) ? 0 : fieldNames.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(super.toString());

    for (String fieldName : fieldNames) {
      result.append("$" + fieldName);
    }

    return result.toString();
  }
}