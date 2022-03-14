/*
The program Tritype (implemented in function foo) takes as 
input three integers (the sides of a triangle) and returns 3 
if the input correspond to an equilateral triangle, 2 if 
it correspond to an isosceles triangle, 1 if it correspond 
to another type of triangle, 4 if it does not correspond 
to a valid triangle. 

The error in this program is in the assignement "trityp = 1",
the instruction should be "trityp = 2". By taking as input 
the values {i=2,j=3,k=2}, the program executes the erroneous 
instruction and returns 1, however, it should return 2 (the input 
correspond to an isosceles triangle).
The program does not contain incorrect conditions. 

SPDX-FileCopyrightText: Mohammed Bekkouche <http://www.i3s.unice.fr>
SPDX-License-Identifier: GPL-3.0-or-later
*/

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
} 


/* program for triangle classification 
 * returns 1 if (i,j,k) are the sides of any triangle
 * returns 2 if (i,j,k) are the sides of an isosceles triangle
 * returns 3 if (i,j,k) are the sides of an equilateral triangle
 * returns 4 if (i,j,k) are not the sides of a triangle
 * 
 
 * an error has been inserted line 54 in the assignment
 * when (i,j,k) = (2,3,2) returns 1 while it would return 2 (an isosceles triangle)
 */
void foo (int i, int j, int k) {//__CPROVER_assume((i == 2) && (j == 3) && (k ==2));
    int trityp;
    if (i == 0 || j == 0 || k == 0) {
        trityp = 4;		
    }
    else {
        trityp = 0;
        if (i == j) {
            trityp = trityp + 1;
        }
        if (i == k) {
            trityp = trityp + 2;
        }
        if (j == k) {
            trityp = trityp + 3;
        }
        if (trityp == 0) {
            if ((i+j) <= k || (j+k) <= i || (i+k) <= j) {
                trityp = 4;				
            }
            else {
                trityp = 1;
            }			
        }
        else {
            if (trityp > 3) {
                trityp = 3;
            }
            else {
                if (trityp == 1 && (i+j) > k) {
                    trityp = 2;
                }
                else {
                    // error in the assignment : trityp = 1 instead of trityp = 2
                    if (trityp == 2 && (i+k) > j) {
                        trityp = 1; // BUG
                    }
                    else {
                        if (trityp == 3 && (j+k) > i) {
                            trityp = 2;
                        }
                        else {
                            trityp = 4;
                        }
                    }
                }
            }
        }
    }


    __VERIFIER_assert(trityp== ((i == 0 || j == 0 || k == 0)?4:(  (i!=j && i!=k && j!=k)?(   ((i+j)<=k || (j+k)<=i || (i+k)<=j)?4:1   ):( ((i==j && j==k) || (j==k && i==k))?3:( (i==j && i!=k && j!=k && (i+j)>k)?2:( (i!=j && j!=k && i==k && (i+k)>j)?2:( ( ((i!=j && j==k && i!=k) || (i==j && j!=k && i==k)) && (j+k)>i)?2:4 ) ) ) ) )) );
    
}

int main() 
{ 
  
  foo( __VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    }
