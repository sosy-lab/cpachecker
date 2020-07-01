int main()
{
  int test_int;

  if ( !(test_int + 256UL) ) {

    goto ERROR;

    ERROR:
      return -1;
  }
  return 0;
}

