int u1;
int u2 = 0, i1 = 0, u3 = 0;
int c = 5;
int *u4;

int func(int y, int z) {
  int x;
  x = f2(x);
  return x + y + z;
}

int f2(int a) {
  return a;
}

void array(int e[]) {
  int *d;
  d[0] = 3;
  e[0] = 3;
  d[e[0]] = 3;
}

int main() {
  array(u4);
  int u5 = u1, i2 = i1, u6 = u6, u7;

  if (u1 == 0) {
     u7 = f2(0);
  }

  u1 += 5;
  u1 += u1;
  u1++;
  *u4 = 0;
  u2 = func(u1, u2);
  u2 = func(5, c);
  u3 = *u4;
  func(u2, u2);
}
