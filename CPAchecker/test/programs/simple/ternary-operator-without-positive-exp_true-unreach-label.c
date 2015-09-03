int main() {
  int x = ( ({
    int __val;
    {
      if (({
        1;
      } )
      == 1)
        __val = 1U;
      switch (4UL)
      {
        case 4:;
        goto ldv_35185;
        default:;
        ldv_35190:;
        goto ldv_35190;
      }
      ldv_35185:;
    }
    __val;
  } ) )
  ? : 1;
  if (x != 1) {
ERROR:
	return 1;
  }
  return 0;
}
