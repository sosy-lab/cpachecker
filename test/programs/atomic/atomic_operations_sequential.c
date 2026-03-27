extern void reach_error(void);
extern void abort(void);

_Atomic int x = 0;
int y = 0;
int z = 0;

int main() {
  x += 1;
  y = ++x;
  z = x++;

  if (y != 2 || z != 2 || x != 3) {
    ERROR: {reach_error();abort();}
  }

  return 0;
}
