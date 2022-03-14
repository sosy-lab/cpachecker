// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack3;

public class SuperType1 {


  protected int  num;
  protected int  num2;

  public SuperType1( int n1 , int n2){

    num = n1;
    num2 = n2;

 }

  public SuperType1( int n){

    num = n;
    num2 = n;

 }

  public SuperType1(){

    num = 0;
    num2 = 0;

 }

  public int getNum1(){
    return num;
  }

  public int getNum2(){
    return num2;
  }

  public int add(){
    return num + num2;
  }

  public boolean co(){
    return num2 == num;
  }

}
