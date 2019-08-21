// this test case checks wether external functions with side-effects are handled correctly
int main() {
	int* p;
	int* q;
	p = malloc(4);
	q = malloc(4);
	if (p != q) {
ERROR:
		goto ERROR;
	}
}
