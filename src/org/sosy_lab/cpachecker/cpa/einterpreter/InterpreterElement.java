/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryFactory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.NonDetProvider;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PersMemory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;

public class InterpreterElement implements AbstractElement {
    private InterpreterElement prev;
    private PersMemory mem;
    private NonDetProvider provider;

    InterpreterElement(int [] mInitialValuesForNondeterministicAssignments){
      BigInteger data[];
      if (mInitialValuesForNondeterministicAssignments == null) {
        data = new BigInteger[0];
      }else{

        data = new BigInteger[mInitialValuesForNondeterministicAssignments.length];
      }
      for(int x =0;x<data.length;x++){
        data[x]= BigInteger.valueOf(mInitialValuesForNondeterministicAssignments[x]);
      }
      provider = new NonDetProvider(data,this);


      mem = new PersMemory(this);
      prev = null;
    }

    public InterpreterElement(PersMemory pMem, NonDetProvider prov) {
      mem = pMem;
      provider = prov;

    }

    InterpreterElement copy(){
        InterpreterElement h = new InterpreterElement(mem,provider);
        h.prev = this;
        return h;
    }


    public MemoryFactory getFactory(){
      return mem.getFactory();
    }

    public Scope getCurrentScope(){
      return mem.getCurrentScope(this);
    }

    public void setCurrentScope(String name){
      mem.setCurrentScope(name, this);
    }

    public void redScope(){
      mem.redScope(this);
    }

    public InterpreterElement getprev(){
      return prev;
    }

    public BigInteger getNonDetNumber() throws Exception {
      // TODO Auto-generated method stub
      return provider.getValue(this);
    }

}

