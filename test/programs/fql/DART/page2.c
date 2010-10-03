
void abort();

int f(int x) 
{ 
  int result;

  result = 2 * x;

  return (result); 
}

int h(int x, int y) 
{
  int tmp;

  if (x != y)
  {
    tmp = f(x);

    if (tmp == x + 10)
    {
ERROR: ;
      abort();  /* error */
    }
  }

  return (0);
}

