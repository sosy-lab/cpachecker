_Noreturn void f() {
LOOP:
  goto LOOP;
}

void main() {
  f();
}
