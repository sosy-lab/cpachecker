# 1 "Global_Initialization1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Global_Initialization1/main.c"
int x = 123;
int y;


typedef unsigned char uchar;
uchar b[] = "abc";


int *p=&y;


extern struct _IO_FILE_plus _IO_2_1_stdin_;

int some_func()
{
  static int some_static;
  return some_static;
}

int main()
{
  assert(x == 123);
  assert(y == 0);
  assert(b[0]=='a');
  assert(some_func()==0);
  assert(p==&y);
}
