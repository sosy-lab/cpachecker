# 1 "Float14/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float14/main.c"
int main()
{
  float temp;

  temp = 1.8e307f + 1.5e50f;
  assert(__CPROVER_isinf(temp));

  float x;

  x=temp-temp;


  assert(__CPROVER_isinf(temp));
}
