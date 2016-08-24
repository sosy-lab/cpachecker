# 1 "const_ptr1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "const_ptr1/main.c"
const char s[]="abc";

const char *my_array[]=
{
  "xyz",
  0
};

int main()
{
  const char *p1;
  p1=s;



  const char * const *p2;
  p2=my_array;

  const char *p3;
  char ch;

  p3=*p2;

  ch=*p3;

  assert(ch=='x');
}
