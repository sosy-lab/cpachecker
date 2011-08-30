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
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.PointerType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;


public class PointerVariable implements Variable {
  boolean isConst; //TODO: unhandeld
  String name;

  int level;
  PointerType typ;
  Type basetyp;
  InterpreterElement tmp;

  Address addr;

 /* public PointerVariable(String pname,Address paddr, Scope pscope, Type pbasetyp,int plevel){
    name = pname;
    addr = paddr;
    pscope = scope;
    basetyp = pbasetyp;
    level = plevel;

    typ = new PointerType(pbasetyp, isConst, plevel);
  }*/


  public PointerVariable(String pname,Address paddr,  Type pbasetyp, PointerType ptyp,int plevel){
    name = pname;
    addr = paddr;

    basetyp = pbasetyp;
    level = plevel;

    typ = ptyp;

  }




  @Override
  public TypeClass getTypeClass(InterpreterElement pel) {
    // TODO Auto-generated method stub
    return TypeClass.POINTER;
  }

  @Override
  public Type getType() {
    // TODO Auto-generated method stub
    return typ;
  }



  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return name;
  }

  @Override
  public Address getAddress() {
    // TODO Auto-generated method stub
    return addr;
  }

  @Override
  public int getSize() {
    // Platform specific
    return 4;
  }

  public int getlevel(){
    return level;
  }

  public Type getBaseType(){
    return basetyp;
  }


  @Override
  public boolean isConst() {
    // TODO Auto-generated method stub
    return isConst;
  }




  @Override
  public void copyVar(String pname, InterpreterElement el) throws Exception {
    // TODO Auto-generated method stub
    MemoryBlock b =el.getFactory().allocateMemoryBlock(this.getSize(),el);
    Address naddr = new Address(b, 0);

    PointerVariable nvar = new PointerVariable(pname, naddr, basetyp, typ, level );
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



  public boolean isNullPointer(InterpreterElement pel) throws MemoryException{
       return addr.getMemoryBlock().getAddress(addr.getOffset(),pel).getMemoryBlock()==null;
  }

  public void setNullPointer(InterpreterElement pel){

    try {
        addr.getMemoryBlock().setAddress(addr.getOffset(), new Address(null,0),pel);
    } catch (RuntimeException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }


  }






}
