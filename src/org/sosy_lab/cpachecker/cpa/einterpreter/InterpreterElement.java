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


import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryFactory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PersMemory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;

public class InterpreterElement implements AbstractElement {
    InterpreterElement prev;
    PersMemory mem;

    InterpreterElement(){
      mem = new PersMemory(this);
      prev = null;
    }

    public InterpreterElement(PersMemory pMem) {
      mem = pMem;
    }

    InterpreterElement copy(){
        InterpreterElement h = new InterpreterElement(mem);
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

}

