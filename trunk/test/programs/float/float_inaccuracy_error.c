// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This C program doesn't enter the error path.
// But the associated Java program does enter the error path,
// java code given in comment.

int main() {
   int i = 0;
   int j = 123;
   float b = 2.2;
   double h = j + b; // 125.2
   
   double x = 125.2 - h;
   if (x > 0.0) {
      goto Error;
   }
   return 0;

   Error:
   return -1;

}

/*
public class Test {
	public static void main(String[] args) {
		   int i = 0;
		   int j = 123;
		   float b = (float) 2.2;
		   double h = j + b; // 125.2
		   
		   double x = 125.2 - h;
		   if (x > 0.0) {
		      System.out.println("Java behaves differently from C.");
		   } else {
			   System.out.println("Java behaves the same as C");
		   }
	}
}
*/
