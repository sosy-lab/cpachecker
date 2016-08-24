# 1 "Pointer_difference1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_difference1/main.c"
int main()
{
  unsigned int arrayOfInt [2];

  int diff1 = &arrayOfInt[1] - arrayOfInt;
  __CPROVER_assert(diff1==1, "pointer diff1");

  unsigned int diff2 = &arrayOfInt [1] - arrayOfInt;
  __CPROVER_assert(diff2==1, "pointer diff2");

  return 0;
}
