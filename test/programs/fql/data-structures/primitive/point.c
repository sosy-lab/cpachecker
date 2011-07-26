

int main(){
  int x = 55;
  short * test;
  short * k;
  test = &x;
  *test = *test+5;
  test = test+1;
  *test = *test+1;

  short z = x;
  x = z+3600;

  
  test = 0;
  if(test==0){
    x = 45+6;
  }
  test=&x;
  k=&x;
  if(test==k){
    x+=4;
  }

  x = 3>=x;
  x ++;
}
