# 1 "Boolean_Guards1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Boolean_Guards1/main.c"
int main() {
  unsigned x;
  int i;
  int a[100];


  if(x<100 && a[x])
  {
    i++;
  }

  __CPROVER_assume(i<100);


  if(i>=0 && a[i])
  {
    i++;
  }
}
