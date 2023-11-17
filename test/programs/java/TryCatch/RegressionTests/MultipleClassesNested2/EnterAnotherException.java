// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class EnterAnotherException {

    private EnterException e;
    private Boolean entered = false;

    public EnterAnotherException(){
        e = new EnterException();
    }

    public void throwException() {
        
        try{
            e.throwException();
        } catch(NullPointerException e){
            entered = true;
        }
        assert entered;
    
        throw new ArrayIndexOutOfBoundsException(); 
    }
}
