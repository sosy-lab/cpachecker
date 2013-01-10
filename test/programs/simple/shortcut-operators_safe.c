int error() {
	// this function is actually never called
	int p;
	if (p) {
ERROR:
		goto ERROR;
	}
}

int main() {
	const int t = 1;
	const int f = 0;
	int tmp;

	if (t || error()) { }
	if (f && error()) { }

	tmp = t || error();
	tmp = f && error();

	if (t > (f && error())) { }
	if (f < (t || error())) { }
	
	tmp = t > (f && error());
	tmp = f < (t || error());

	if (!!!(t || error())) { }
	if (!!!(f && error())) { }
}
