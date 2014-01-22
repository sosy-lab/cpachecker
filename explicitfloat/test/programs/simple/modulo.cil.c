int main(void) 
{
  int i;

  {
    i = i % 2;
    if (i == 1) {
      goto ERROR;
ERROR: ;
    } else {
    }
  }
}
