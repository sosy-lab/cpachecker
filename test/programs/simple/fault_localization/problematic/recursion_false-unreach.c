int main() {
	int x = 0;
	x++;
	if (x == 1) {
		int i = 0;
		int j = 2;
		while(i != j)
			i++;
		goto ERROR;
	}
	
EXIT: return 0;
ERROR: return 1;
}

