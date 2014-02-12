extern int __VERIFIER_nondet_int();

typedef struct fraction {
   int c ;
   int d;
};


int flag = 1;
int inter;

// approximation of behavior, only important when it returns 0
int gcd(int x, int y){
    if(x!=0 || y!=0){
	return 1;
    }
    
    return 0;
}

void reduceFraction(struct fraction frac){
    inter = gcd(frac.c, frac.d);
    flag = inter;
    frac.c = frac.c/inter;
    flag = inter;
    frac.d = frac.d/inter; 
}

void main(){
  struct fraction frac, frac2;
  frac.c = __VERIFIER_nondet_int();
  do{
     frac.d = __VERIFIER_nondet_int();
  }while(frac.d == 0);

  if(frac.c != 0){
    frac2.c = frac.d;
    frac2.d = frac.c;
    reduceFraction(frac2);
  }  

  reduceFraction(frac);
}

