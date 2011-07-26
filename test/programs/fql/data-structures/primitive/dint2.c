

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
