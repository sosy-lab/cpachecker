//Test should check, how the tool handle the skipped variables due to annotations
int errno;
int true_unsafe;
int true_unsafe2;
int true_unsafe3;
int true_unsafe4;

struct mystruct {
	int* a;
} S;

typedef struct mystruct __my;

__my S2;

int main() {
  errno = 0;
  S->a = 0;
  S2->a = 0;
}

int ldv_main() {
	main();
}
