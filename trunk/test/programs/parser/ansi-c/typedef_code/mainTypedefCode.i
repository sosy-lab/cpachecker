# 1 "typedef_code/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "typedef_code/main.c"
typedef int CODETYPE (int a);

extern CODETYPE func;

int func(int a)
{
  return 1;
}

int main(void)
{
  return func(2);
}
