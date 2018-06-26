int main() {
int array[2] = {1, 2};
int i = 0;
int a = 0;


  while (i < 2) {
    if (array[i] == 2) {
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

  if (a != 1) {
     goto ERROR;
  }

  return (0);
  ERROR:
  return (-1);
}

