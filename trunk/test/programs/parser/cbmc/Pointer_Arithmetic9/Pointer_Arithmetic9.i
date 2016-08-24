# 1 "Pointer_Arithmetic9/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic9/main.c"
unsigned short array[4];

int main(void)
{
  unsigned short *buf;

  buf=array;


  *(((unsigned long *)buf)++);

  return 0;
}
