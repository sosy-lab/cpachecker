int main() {
	int x = 5;
	switch (5) {
	default: // fall-through after default
	case 1:
	case 2:
ERROR:
		return 1;
	case 3:
		return 0;
	}
}	
