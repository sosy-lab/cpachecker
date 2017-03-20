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



public class BinaryIdentifier implements AbstractIdentifier {
  protected AbstractIdentifier id1;
  protected AbstractIdentifier id2;
  protected int dereference;

  public BinaryIdentifier(AbstractIdentifier i1, AbstractIdentifier i2, int deref) {
    id1 = i1;
    id2 = i2;
    dereference = deref;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dereference;
    result = prime * result + ((id1 == null) ? 0 : id1.hashCode());
    result = prime * result + ((id2 == null) ? 0 : id2.hashCode());
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
    BinaryIdentifier other = (BinaryIdentifier) obj;
    if (dereference != other.dereference) {
      return false;
    }
    if (id1 == null) {
      if (other.id1 != null) {
        return false;
      }
    } else if (!id1.equals(other.id1)) {
      return false;
    }
    if (id2 == null) {
      if (other.id2 != null) {
        return false;
      }
    } else if (!id2.equals(other.id2)) {
      return false;
    }
    return true;
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
    info += "(" + id1.toString() + " # " + id2.toString() + ")";
    return info;
  }

  @Override
  public boolean isGlobal() {
    return (id1.isGlobal() || id2.isGlobal());
  }

  @Override
  public BinaryIdentifier clone() {
    return new BinaryIdentifier(id1.clone(), id2.clone(), dereference);
  }

  public AbstractIdentifier getIdentifier1() {
    return id1;
  }

  public AbstractIdentifier getIdentifier2() {
    return id2;
  }

  @Override
  public int getDereference() {
    return dereference;
  }

  @Override
  public boolean isPointer() {
    if (dereference != 0) {
      return true;
    } else
      if ( id1.isPointer() && id2.isPointer()) {
        return true;
      } else if (!id1.isPointer() && !id2.isPointer()) {
        return false;
      } else {
        //strange
        return true;
      }
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
      int deref = id1.getDereference();
      AbstractIdentifier tmp1 = id1.clone();
      AbstractIdentifier tmp2 = id2.clone();
      tmp1.setDereference(dereference + deref);
      deref = id2.getDereference();
      tmp2.setDereference(dereference + deref);
      AbstractIdentifier id1Container = tmp1.containsIn(pSet);
      AbstractIdentifier id2Container = tmp2.containsIn(pSet);
      if (id1Container != null) {
        return id1Container;
      } else if (id2Container != null) {
        return id2Container;
      } else {
        return null;
      }
    }
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof SingleIdentifier) {
      return -1;
    } else if (pO instanceof BinaryIdentifier) {
      int result = this.id1.compareTo(((BinaryIdentifier)pO).id1);
      return (result != 0 ? result : this.id2.compareTo(((BinaryIdentifier)pO).id2));
    } else {
      return 1;
    }
  }

  @Override
  public DataType getType(Map<? extends AbstractIdentifier, DataType> pLocalInfo) {
    /*AbstractIdentifier tmp = name.containsIn(localInfo.keySet());
    DataType result1 = (tmp == null ? null : localInfo.get(tmp));*/
    int deref = id1.getDereference();
    AbstractIdentifier tmp1 = id1.clone();
    //AbstractIdentifier tmp2 = ((BinaryIdentifier) name).getIdentifier2().clone();
    tmp1.setDereference(dereference + deref);
    /*deref = ((BinaryIdentifier) name).getIdentifier2().getDereference();
    tmp2.setDereference(((BinaryIdentifier) name).getDereference() + deref);*/
    return tmp1.getType(pLocalInfo);
    //DataType type2 = getType(localInfo, tmp2);
    //return DataType.max(type1, type2);
    //return result2;
  }
}
