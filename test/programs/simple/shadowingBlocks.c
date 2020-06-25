int main(void)
{

  int i;
  i = 0;

  {
    {
      int i;
      i = 1;
    }

    int i;
    i = 2;

    {
      int i;
      i = 3;
    }
  }

  return 0;
}
