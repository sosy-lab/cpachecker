// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int l[]={0,1};
int main(){
     int*p=l;
     if(*p++ == 0) return 0;
     {ERROR:goto ERROR;}
     return 0;
}
