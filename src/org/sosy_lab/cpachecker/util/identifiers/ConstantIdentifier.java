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

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;



public class ConstantIdentifier implements AbstractIdentifier {

  protected String name;
  protected int dereference;

  public ConstantIdentifier(String nm, int deref) {
    name = nm;
    dereference = deref;
  }

  @Override
  public ConstantIdentifier clone() {
    return new ConstantIdentifier(name, dereference);
  }

  @Override
  public String toString() {
    String info = "";
    if (dereference > 0) {
      for (int i = 0; i < dereference; i++) {
        info += "*";
      }
    } else if (dereference == -1) {
      info += "&";
    } else if (dereference < -1){
      info = "Error in string representation, dereference < -1";
      return info;
    }
    info += name;
    return info;
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public int getDereference() {
    return dereference;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
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
    ConstantIdentifier other = (ConstantIdentifier) obj;
    if (dereference != other.dereference) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isPointer() {
    return (dereference > 0);
  }

  @Override
  public void setDereference(int pD) {
    dereference = pD;
  }

  @Override
  public AbstractIdentifier containsIn(Collection<? extends AbstractIdentifier> pSet) {
    if (pSet.contains(this)) {
      return this;
    } else {
      return null;
    }
  }

  public String getName() {
    return name;
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof ReturnIdentifier) {
      return 1;
    } else if (pO instanceof ConstantIdentifier) {
      return this.name.compareTo(((ConstantIdentifier)pO).name);
    } else {
      return -1;
    }
  }

  @Override
  public DataType getType(Map<? extends AbstractIdentifier, DataType> pLocalInfo) {
    if (isPointer() && !(name.equals("0"))) {
      return DataType.GLOBAL;
    } else {
      return DataType.LOCAL;
    }
  }

}
