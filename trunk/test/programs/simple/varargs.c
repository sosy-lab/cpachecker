void f1(int nargs, __builtin_va_list l)
{
  int a;
  
  for (;nargs > 0;nargs--)
  {
    a = __builtin_va_arg(l, int);
    printf("%i ", a);
  }
}

void f(int nargs, ...)
{
  __builtin_va_list l;
  
  __builtin_va_start(l, f);
  f1(nargs, l);
  __builtin_va_end(l);
}

int main()
{
  f(3, 1, 2, 3); 
}

