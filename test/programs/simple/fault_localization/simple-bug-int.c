int main() {

	int a = 0;
	int b = -2;
	int x = 1;
	x = x + a;   //  1
	x = x + b;   // -1
	x = x + 2;   //  1
	x = x - 2;   // -1
	x = x + 0;   // -1
	x = x + -2;  // -3

	if (x < 0)
		goto ERROR;

EXIT: return 0;
ERROR: return 1;
}
