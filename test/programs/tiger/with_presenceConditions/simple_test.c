extern int input();

int main() {
	int a = input();
	int b = input();
	int c;

	if (a + b > 0)
		G1: c = a + b;
	else {
		G2: c = a - b;
		G3: c *= a;
	}
	
	G4: c += b;

	return 0;
}
