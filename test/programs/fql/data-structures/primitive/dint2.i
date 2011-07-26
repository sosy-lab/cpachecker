# 1 "dint2.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "dint2.c"


int a;

int main(){
  a=300;
  int b;
  b = (int) &a;
  b=b+1;
  *((char*)b)=4;
  b=b+1;
  *((short*)b)=735;

}
