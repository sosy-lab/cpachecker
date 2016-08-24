# 1 "Visual_Studio_Types1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Visual_Studio_Types1/main.c"
int main()
{

  __int8 i1;
  __int16 i2;
  __int32 i3;
  __int64 i4;

  assert(sizeof(i1)==1);
  assert(sizeof(i2)==2);
  assert(sizeof(i3)==4);
  assert(sizeof(i4)==8);



  char c;
  short s;
  int i;
  long l;
  long long ll;

  assert(sizeof(c)==1);
  assert(sizeof(s)==2);
  assert(sizeof(i)==4);
  assert(sizeof(l)==4);
  assert(sizeof(ll)==8);


  assert(sizeof(1i8)==1);
  assert(sizeof(1i16)==2);
  assert(sizeof(1i32)==4);
  assert(sizeof(1i64)==8);
  assert(sizeof(1i128)==16);


  int * __ptr32 p32;



  assert(sizeof(p32)==4);

  assert(sizeof(void *)==4);
}
