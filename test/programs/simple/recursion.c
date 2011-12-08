void recursive(int i) {
	if (i > 0) {
		i--;
		recursive(i);
	}
}

void main() {
	recursive(10);
}
