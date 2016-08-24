# 1 "Pointer_Arithmetic3/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Pointer_Arithmetic3/main.c"
int nums[2];
int *p;

int main() {
  nums[1] = 1;
  p = &nums[0];
  p++;

  assert(*p == 1);
}
