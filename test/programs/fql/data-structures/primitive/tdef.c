typedef void test();



void tfunc(){

}
void (*jj)();
test *sum(){
  jj = tfunc;
  return jj;

}


test* (*pnt)();
int main(){
  int k = 4;
  pnt  = sum;



}
