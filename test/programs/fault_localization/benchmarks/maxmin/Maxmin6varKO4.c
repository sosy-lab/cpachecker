/*
The program Maxmin6var (implemented in function foo) takes 
six poxisitve values as input and assigns to the variable max 
the maximum value and to the variable min the minimum value. 

There is three errors in this program. By taking the following 
input {a=1, b=-3, c=-4, d=-2, e=-1, f=-2}, the program outputs 
 {max=-1,min=-4} instead of {max=1,min=-4}. 

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

/*
 * Find the maximum and minimum of six values.
 */
void foo (int a, int b, int c, int d, int e, int f) {
	int max;
    int min;
	if ((a>b) && (a>c) && (a>d) && (a>e) && (a>f)){ 
        max=a;
        /*Calculer le minimum*/
        if ((b>c) && (b<d) && (b<e) && (b<f)){ // error, the instruction should be ("(b<c)" && (b<d) && (b<e) && (b<f))
            min=b;
        }
        else{
            if ((c<d) && (c<e) && (c<f)){
                min=c;
            }
            else{
                if ((d<e) && (d<f)){
                    min=d;
                }
                else{
                    if (e<f){
                        min=e;
                    }
                    else{
                        min=f;
                    }
                }
            }  
        }
        
	}else{ 
	    if ((b>c) && (b>d) && (b>e) && (b>f)){
            max=b;
            
            if ((a<c) && (a<d) && (a<e) && (a<f)){
                min=a;
            }else{
                if ((c<d) && (c<e) && (c<f)){
                    min=c;
                }
                else{
                    if ((d<e) && (d<f)){
                        min=d; 
                    }
                    else{
                        if (e<f){
                            min=e;
                        }
                        else{
                            min=f;
                        }
                    }
                }  
            }
            
        }    
	    else{
            if ((c>d) && (c>e) && (c>f)){ 
                max=c;
                
                if ((a<b) && (a<d) && (a<e) && (a<f)){
                    min=a;
                }else{
                    if ((b<d) && (b<e) && (b<f)){
                        min=b;
                    }
                    else{
                        if ((d<e) && (d<f)){
                            min=d; 
                        }
                        else{
                            if (e<f){
                                min=e;
                            }
                            else{
                                min=f;
                            }
                        }
                    }  
                }
                
            }
            else{
                if ((d>e) && (d>f)){
                    max=d;
                    
                    if ((a<b) && (a<c) && (a<e) && (a<f)){
                        min=a;
                    }else{
                        if ((b<c) && (b<e) && (b<f)){
                            min=b;
                        }
                        else{
                            if ((c<e) && (c<f)){
                                min=c; 
                            }
                            else{
                                if (e<f){
                                    min=e;
                                }
                                else{
                                    min=f;
                                }
                            }
                        }  
                    }
                }
                else{
                    if (e>f){
                        max=e;
                        
                        if ((a<b) && (a<c) && (a<d) && (a<f)){
                            min=a;
                        }else{
                            if ((b<c) && (b<d) && (b<f)){
                                min=b;
                            }
                            else{
                                if ((c<d) && (c<f)){
                                    min=c; 
                                }
                                else{
                                    if (d<f){
                                        min=d;
                                    }
                                    else{
                                        min=f;
                                    }
                                }
                            }  
                        }       
                    }
                    else{
                        max=f;
                        
                        if ((a<b) && (a<c) && (a<d) && (a<e)){
                            min=a;
                        }else{
                            if ((b<c) && (b<d) && (b<e)){
                                min=b;
                            }
                            else{
                                if ((c<d) && (c<e)){
                                    min=c; 
                                }
                                else{
                                    if (d<e){
                                        min=d;
                                    }else{
                                        min=e;
                                    }
                                }
                            }  
                        } 
                    }
                }
            }
	    }
    }
    __VERIFIER_assert( (max >= a) && (max >= b) && (max >= c) && (max >= d) && (max >= e) && (max >= f) && (min <= a) && (min <= b) && (min <= c) && (min <= d) && (min <= e) && (min <= f));
}



int main() 
{ 
  
  foo( __VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    return 0; 
} 
