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
package org.sosy_lab.cpachecker.cpa.einterpreter;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Address;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Variable;



public class ExprResult {
  private Address addr;
  private Variable var;
  private byte data[];
  private Type dattyp;
  private RType typ;
  private BigInteger big;
  private int level;
  private CFAFunctionDefinitionNode nod;
  enum RType {
    Addr,
    Var,
   // Data, //TODO: unused remove
    Num,
    FuncPnt;
  }
  ExprResult(CFAFunctionDefinitionNode pnod){
    nod = pnod;
   typ = RType.FuncPnt;
  }

  ExprResult(Address paddr, Type ptype, int plevel){
    typ = RType.Addr;
    addr = paddr;
    level = plevel;
    dattyp = ptype;
  }


  ExprResult(Variable pvar){
    typ = RType.Var;
    var = pvar;
  }


  /* ExprResult(byte []pdata, Type pdattyp){
     typ = RType.Data;
     data = pdata;
     dattyp = pdattyp;
   }*/

   ExprResult(BigInteger pvalue){
     big = pvalue;
     typ = RType.Num;
   }


   Address getAddress() throws ResultException{
       if(typ == RType.Addr)
         return addr;
       else
         throw new ResultException("ExpResult does not contain addr");
   }

   int getLevelofIndirection() throws ResultException{
     if(typ == RType.Addr){
       return level;
     }else
       throw new ResultException("ExpResult does not contain addr");
   }

   Variable getVariable() throws ResultException{
     if(typ == RType.Var)
       return var;
     else
       throw new ResultException("ExpResult does not contain variable");
   }

 /*  byte[] getData() throws ResultException{

     if(typ == RType.Data )
       return data;
     else
       throw new ResultException("ExpResult does not contain data");

   }*/


   Type getDataType() throws ResultException{

     if(/*typ == RType.Data||*/ typ == RType.Addr)
       return dattyp;
     else
       throw new ResultException("ExpResult does not contain data");

   }

   BigInteger getnumber() throws ResultException{

     if(typ == RType.Num)
       return big;
     else
       throw new ResultException("ExpResult does not contain number");

   }


  RType getResultType(){
    return typ;
  }

  CFAFunctionDefinitionNode getFunctionPnt(){
    return nod;
  }


}
