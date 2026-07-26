int ldv_mutex = 1;

void main(void)
{
  int tmp___7 ;
  int tmp___8 ;
  while (1) {
    tmp___8 = __VERIFIER_nondet_int();
    if (!tmp___8)
      break;
    tmp___7 = __VERIFIER_nondet_int();
//    if (tmp___7 == 0){}
  }
  if (ldv_mutex != 1)
     reach_error();
  return;
}

