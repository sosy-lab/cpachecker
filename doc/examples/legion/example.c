int main() {
  int i = 0;
  int x = __VERIFIER_nondet_int();
  int a = 0;

  while (1) {
    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       x++;
       a++;
    }

    if (i != a) {
      goto ERROR;
    }
  }

  LOOPEND:

  if (a != 20) {
     goto ERROR;
  }

  return (0);
  ERROR:
  return (-1);
}

