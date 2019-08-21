# 1 "BV_Arithmetic6/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "BV_Arithmetic6/main.c"
int main()
{
  {
    unsigned i, j, k, l;

    j=k;
    i=j/2;
    l=j>>1;
    assert(i==l);

    j=k;
    i=j%2;
    l=j&1;
    assert(i==l);
  }

  {
    signed int i, j, k, l;


    __CPROVER_assume(!(k&1));
    j=k;
    i=j/2;
    l=j>>1;
    assert(i==l);

    j=k;
    i=j%2;
    l=j&1;
    assert(i==l);

  }
}
