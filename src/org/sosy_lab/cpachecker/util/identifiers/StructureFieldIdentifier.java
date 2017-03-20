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

import org.sosy_lab.cpachecker.cfa.types.c.CType;



public class StructureFieldIdentifier extends StructureIdentifier {
  protected String fieldType;

  public StructureFieldIdentifier(String pNm, /*String fTp,*/ CType pTp, int dereference, AbstractIdentifier own) {
    super(pNm, pTp, dereference, own);
    if ( type != null ){
      fieldType = type.toASTString("");
    } else {
      fieldType = "";
    }
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
    info += "(?.";
    info += name;
    info += ")";
    return info;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    StructureFieldIdentifier other = (StructureFieldIdentifier) obj;
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public StructureFieldIdentifier clone() {
    return new StructureFieldIdentifier(name, /*fieldType,*/ type, dereference, owner);
  }

  @Override
  public SingleIdentifier clearDereference() {
    return new StructureFieldIdentifier(name, /*fieldType,*/ type, 0, owner);
  }


  @Override
  public String toLog() {
    return "f;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return new GeneralStructureFieldIdentifier(name, /*fieldType,*/ type, dereference, owner);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof GlobalVariableIdentifier || pO instanceof LocalVariableIdentifier) {
      return -1;
    } else if (pO instanceof StructureFieldIdentifier){
      int result = super.compareTo(pO);
      if (result != 0) {
        return result;
      } else {
        return this.fieldType.compareTo(((StructureFieldIdentifier) pO).fieldType);
      }
    } else {
      return 1;
    }
  }

}
