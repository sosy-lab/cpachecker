// Based on a program by Chengyu Zhang for #422

int main() {
  int tmp[2];
  int *p, *q;
  p = tmp;
  q = p + 1;
  *q = 0;
  tmp[1] = 39;
  if (*q == 39) {
ERROR:
    goto ERROR;
  }
  return 0;
}
