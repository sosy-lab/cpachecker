extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int(void);

int p = 0;
int b = 0;
int w = 1;
int c = 0;

int is(void) { if (w < 2) { return 1; } else { return 0; } }
void set(void) { p = 1; }
int get(void) { return p; }
int not(void) { return !is(); }
void S2(void) { b = get(); } // <-- return-value of a functioncall is assigned
void S4(void) { if (!p) { if (not()) { if (!c) { set(); } } } }
void S3(void) {
  if (p && w == 0) { p = 0; } else { S4(); }
  if (w != 2 && p) { if (!b) { ERROR: __VERIFIER_error(); } }
}
void main(void) {
  int t = 0;
  while (t == 0) {
    if (__VERIFIER_nondet_int()) { if (w < 2) { w++; } }
    if (__VERIFIER_nondet_int()) { c = !c; } else { }
    S2();
    if (p && w > 0) { w = w - 1; }
    S3();
  }
  S3();
}

