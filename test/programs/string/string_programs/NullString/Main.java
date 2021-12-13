// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

public class Main {
    
  public static String empty() {
    return null;
    }
    
  @SuppressWarnings("null")
  public static void main(String[] args) {
        
    String s1 = null;
        
    try {
      s1.equals("hello");
      } catch (NullPointerException e) {
        assert true; 
        }
    try {
      empty().length();
      }catch (NullPointerException e) {
        assert true; 
        }
        
        String s2 = "notNull";
        String result = s1+s2;
        assert result.equals(s2);
    }

}