# 1 "Float7/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float7/main.c"
int main()
{
  unsigned int i;
  i=0;

  float *p;
  p=(float *)&i;

  float f=*p;

  assert(f==0.0);
}
