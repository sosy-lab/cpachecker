# 1 "Unwinding_Locality1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Unwinding_Locality1/main.c"
int main() {
  int i;

  for(i=0; i<10; i++) {
    const int a=i;
  }

  int array[10];
  for(i=0; i<10; i++)
  {
    const int a;
    array[i]=a;
  }


  assert(array[0]==array[1]);
}
