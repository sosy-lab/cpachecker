struct F { int x;};
int main()
{
  int timeout = 0;
  int x = 0;
  while (1)
    {
      const struct F i = { x++};
      if (i.x > 0)
break;
      if (++timeout > 5)
{ERROR:goto ERROR;}
    }
  return 0;
}
