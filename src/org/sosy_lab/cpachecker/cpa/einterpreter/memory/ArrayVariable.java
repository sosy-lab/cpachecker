/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.ArrayType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;






public class ArrayVariable  implements Variable{
  ArrayVariable clone=null;
  ArrayType type;
  Type baseType;
  int length;
  Address addr;
  String name;
  InterpreterElement tmp;


  public ArrayVariable(String pname, Address paddr, int plength, ArrayType ptype, Type pbaseType  ){
      name = pname;
      addr = paddr;
      length = plength;
      type = ptype;
      baseType= pbaseType;
  }


  @Override
  public TypeClass getTypeClass(InterpreterElement pel) {
    return TypeClass.ARRAY;
  }

  @Override
  public Type getType() {
    return type;
  }

  public Type getBaseType(){
    return baseType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Address getAddress() {
    return addr;
  }

  @Override
  public int getSize() {
    return baseType.sizeOf()*length;
  }

  @Override
  public void copyVar(String pname, InterpreterElement el) throws Exception {
    MemoryBlock b =el.getFactory().allocateMemoryBlock(this.getSize(),el);
    Address naddr = new Address(b, 0);

    ArrayVariable nvar = new ArrayVariable(pname, naddr,length,type,baseType );
    MemoryBlock oldb=addr.getMemoryBlock();
    int of= addr.getOffset();

    for(int x=0; x<nvar.getSize();x++){
      MemoryCell data = oldb.getMemoryCell(of+x,el);
      if(data != null && data instanceof AddrMemoryCell){
        data = data.copy();
      }
      b.setMemoryCell(data,x,el);

    }
    el.getCurrentScope().addVariable(nvar,el);

  }

  @Override
  public boolean isConst() {
    // TODO Auto-generated method stub
    return false;
  }

  /*@Override
  public ArrayVariable clone(){
    if(clone==null){
      Address v = addr.clone();

      clone= new ArrayVariable(name, v,this.length,type,baseType);
    }
    return clone;
  }*/




}
