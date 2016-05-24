extern int __VERIFIER_nondet_int(void);

int flag=1;

int main(){
    int y = __VERIFIER_nondet_int();
    int x=0;
    int z=1;
    
    if(y<2)
    {
      return 0;
    }

    while(z<=y)	
    {
      z=z*2;
      x=x+1;
    }
    
    flag = x;
    
	return x;
}


