# 1 "Struct_Initialization2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Struct_Initialization2/main.c"
struct teststr {
   int a;
   int b;
   int c;
};

struct teststr str_array[] = {
  { .a = 3 },
  { .b = 4 }
};

int main()
{
  assert(str_array[0].a==3);
  assert(str_array[0].b==0);
  assert(str_array[1].b==4);

  int x;


  str_array[0] = (struct teststr){ .a=1, .c=x };
  assert(str_array[0].a==1);
  assert(str_array[0].b==0);
  assert(str_array[0].c==x);
}
