extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
void maxMin(int a, int b, int c, int d, int e, int f) {//__CPROVER_assume((a==1) && (b==-4) && (c==-3) && (d==-1) && (e==0) && (f==-4));
	int max;
    int min;
	if ((a>b) && (a>c) && (a>d) && (a>e) && (a>f)){
        max=a;
        
        if ((b<c) && (b<d) && (b<e) && (b<f)){
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
                    if (e>f){ // the instruction should be (e<f)
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
    __VERIFIER_assert((max >= a) && (max >= b) && (max >= c) && (max >= d) && (max >= e) && (max >= f) && (min <= a) && (min <= b) && (min <= c) && (min <= d) && (min <= e) && (min <= f));
}



int main() 
{ 
  
  maxMin(1,-4,-3,-1,0,-4);
    return 0; 
} 