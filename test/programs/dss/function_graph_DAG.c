// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void h(){

}

void g(){
    h();
}

void f(){

    int y;

    if (y){
        return;
    } 

    g();


}

int main(){

    int x;
    f();
    if(x){
        f();
    } else {
        g();
    }

    return 0;

}