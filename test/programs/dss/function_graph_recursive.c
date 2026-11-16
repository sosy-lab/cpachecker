// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int leaf1(){
    return 3;
}

int leaf2(){
    return 3;
}

void rec1();

void rec2(){

    int y;

    if (y){
        return;
    } 

    leaf2();
    rec3();
}

void rec3(){
    leaf2();
    
    int y;

    if (y){
        return;
    } 
    rec1();
}

void rec1(){
    int x;

    leaf1();
    if(x){
        rec1();
    } else {
        rec2();
    }
}

int main(){

    int x;
    leaf1();
    leaf1();

    if(x){
        rec1();
    } else {
        rec2();
    }

    return 0;

}