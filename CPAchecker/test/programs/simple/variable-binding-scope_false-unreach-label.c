int main() {
	int x = 5;
	{
		int x = x;
		// the inner x has a non-deterministical value

		if (x == 0) {
			// so this error location is reachable
ERROR:			goto ERROR;
		}
	}
}
