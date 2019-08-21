struct s {
	int x;
};

void f(struct s s) {
	if (s.x != 1) {
		ERROR: return;
	}
}

int main() {
	f((struct s) { .x = 1 });
	return 0;
}
