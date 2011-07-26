# 1 "drecursive.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "drecursive.c"
int test(int a){
  if(a<=13){
    return test(a+2);
  }else{
    return a;
  }

}

int test2(int a){
  return a;

}


int main(){
  int x =4;
  int y =5;
  x = x<y;

  x=x+5;

  int z =1;
  z = test(z);
  z= z+1;

}
