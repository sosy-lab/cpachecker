// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f(int x){

    if(x <= 0)

        return x + 1;

    else 
        
        return x -1;
}


int main(){

    int nodet;

    int x = nodet ? f(1) : f(0);
    
    if(nodet){
        f(1);
    } else {
        f(2);
    }
    

}