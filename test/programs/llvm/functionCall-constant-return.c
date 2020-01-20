extern void __VERIFIER_error();

int f() {
  return 0;
}

int main() {
	int y;
	y = f();
	if (y != 0) {
ERROR:
    __VERIFIER_error();
    return 1;
	}
	return (y);
}
