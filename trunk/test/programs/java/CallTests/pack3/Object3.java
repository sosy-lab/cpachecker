// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack3;

import pack1.pack2.Object2;
import pack1.Object1;

public class Object3 {

  int num1;
  int num2;
  int num3;
  String name;

  public Object3(int num1, int num2, int num3, String name){
    this.num1 = num1;
    this.num2 = num2;
    this.num3 = num3;
    this.name = name;
  }

  public Object1 createAnotherObject(Object2 object2){
    return new Object1(num1, num2, num3, name, object2);
  }

  public int getNum1(){
    return num1;
  }

  public int getNum2(){
    return num2;
  }

  public int getNum3(){
    return num3;
  }

  public String getName(){
    return name;
  }
}
