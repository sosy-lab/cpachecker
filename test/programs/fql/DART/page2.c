
void abort() {

}

int f(int x) 
{ 
  int result;

  result = 2 * x;

  return (result); 
}

int h(int x, int y) 
{
  int tmp;
  int tmp2;
  int tmp3;

  if (x != y)
  {
    tmp = f(x);
    tmp2 = x + 10;
    tmp3 = (tmp == tmp2);

    if (tmp3)
    {
ERROR: ;
      abort();  /* error */
    }
  }

  return (0);
}

