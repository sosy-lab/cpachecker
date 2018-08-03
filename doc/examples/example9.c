int main() {
  int i = 0;
  int a = 0;
int m = 2;

  while (1) {
    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       a++;
    }

    if (i != a) {
      goto ERROR;
    }
  }

  LOOPEND:

  if (m % 2 != 0) {
     goto ERROR;
  }

  return (0);
  ERROR:
  return (-1);
}

