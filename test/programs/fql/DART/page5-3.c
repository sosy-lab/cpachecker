
void abort();

void foobar(int x, int y){
  if (x*x*x > 0)
  {
    if (x > 0)
    {
      if (y == 10)
      {
        abort();
      }
    }
  } 
  else 
  {
    if (x > 0)
    {
      if (y == 20)
      {
        abort();
      }
    }
  }
}

