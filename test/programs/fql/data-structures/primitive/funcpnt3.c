
void test(){
  int x;
  x=3;
}

struct test1{
  int x;
  void (*pnt)();
  
};
int main(){
  struct test1 lab;
  struct test1*k;
  k=&lab;
  k->pnt = test;
  k->pnt();


}
