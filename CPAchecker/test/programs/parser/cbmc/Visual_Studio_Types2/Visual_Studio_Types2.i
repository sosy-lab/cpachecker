# 1 "Visual_Studio_Types2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Visual_Studio_Types2/main.c"
int main()
{

  short s;
  int i;
  long l;
  long long ll;

  assert(sizeof(s)==2);
  assert(sizeof(i)==4);
  assert(sizeof(l)==4);
  assert(sizeof(ll)==8);


  int * __ptr32 p32;
  int * __ptr64 p64;


  assert(sizeof(p32)==4);
  assert(sizeof(p64)==8);
  assert(sizeof(void *)==8);
}
