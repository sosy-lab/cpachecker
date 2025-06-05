typedef struct union_in_struct_s {
  union {
    char a; // 8 bit            00000000
    short b; // 16 bit  0000000100000000 (512)
  };
  int c;
} union_in_struct_t;

int main() {
  union_in_struct_t s;
  s.b = 512;
  __VERIFIER_assert(s.a == 0);
  return 0;
}
