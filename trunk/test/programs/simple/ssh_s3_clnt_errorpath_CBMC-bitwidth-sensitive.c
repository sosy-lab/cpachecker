int main()
{
  int test_int;
  unsigned long test;

  test = (unsigned long)test_int;

  if ( !(test + 256UL) ) {

    goto ERROR;

    ERROR:
      return -1;
  }
  return 0;
}

