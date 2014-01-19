int array_sizes(int array_index);
int array_values(int array_index, int index);

void __CPROVER_assume(int condition);

int array_next_index;

int array_new(int size) 
{
  int array_index;
  int array_size;

  array_index = array_next_index;
  array_next_index = array_next_index + 1;

  array_size = array_sizes(array_index);

  __CPROVER_assume(array_size == size);

  return (array_index);
}

int array_update(int array_index, int index, int value)
{
  int array_size;
  int new_array;
  int i;
  int tmp_value;
  int tmp;

  array_size = array_sizes(array_index);

  // copy array
  new_array = array_new(array_size);

  i = 0;

  while (1)
  {
    if (i >= array_size)
    {
      break;
    }
    else 
    {
    }

    if (i == index)
    {
      // update array
      tmp_value = value;
    }
    else 
    {
      tmp_value = array_values(array_index, i);
    }

    tmp = array_values(new_array, i);

    __CPROVER_assume(tmp_value == tmp);
  }

  return (new_array);
}

int foo(int a) 
{
  a = array_update(a, 0,  0);
  a = array_update(a, 1, 10);
  a = array_update(a, 2, 20);

  return (a);
}

int main(int argc, char * argv[]) 
{
  int A;
  int tmp1;
  int tmp2;
  int tmp3;

  array_next_index = 0;

  A = array_new(3);

  A = foo(A);

  tmp1 = array_values(A, 0);
  tmp2 = array_values(A, 1);

  if (tmp1 > tmp2) 
  {
L:
    tmp3 = 1;
  }
  else
  {
    tmp3 = 0;
  }

  return (tmp3);
}

