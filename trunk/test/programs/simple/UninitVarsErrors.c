
int f1() {
	int x;
	return x;
}


int main() {
	// Trigger UNINITIALIZED_RETURN_VALUE and UNINITIALIZED_VARIABLE_USED
	int y;
	y = f1();

	return (0);
	ERROR: return (-1);
}
