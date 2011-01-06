
void abort() {

}

struct foo 
{ 
  int i; 
  char c; 
};

void bar (struct foo *a) 
{
  char a_c;
  char* a_c_addr;
  int tmp1;
  int tmp2;

  a_c = a->c;
  tmp1 = (a_c == 0);

  if (tmp1)
  {
    a_c_addr = (char *)a + sizeof(int);
    *a_c_addr = 1;

    a_c = a->c;
    tmp2 = (a_c != 0);

    if (tmp2)
    {
      abort();
    }
  }
}

