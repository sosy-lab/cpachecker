enum e {
	E1,
	E2 = E1 + 3,
	E3
};

int main() {
	if (E1 != 0) {
		goto ERROR;
	}
	if (E2 != 3) {
		goto ERROR;
	}
	if (E3  != 4) {
		goto ERROR;
	}

	return 0;
ERROR:
	return 1;
}
