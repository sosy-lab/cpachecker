// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package pack3;

public class SubSubType2 extends SubType1 {


  int subsubNum1;
  int subsubNum2;


  public SubSubType2(){
    super();
    subsubNum1 = 0;
    subsubNum2 = 0;
  }


  public SubSubType2(int num1 , int num2){
    super();
    subsubNum1 = num1;
    subsubNum2 = num2;
  }

  public int getNum1(){
    return subsubNum1;
  }

  public int getNum2(){
    return subsubNum2;
  }

}
