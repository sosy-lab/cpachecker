int main(void)
{

  int i;
  i = 0;

  {
    int i;
    i = 2;

    int i; // should lead to an error
    i = 3;
  }

  return 0;
}
