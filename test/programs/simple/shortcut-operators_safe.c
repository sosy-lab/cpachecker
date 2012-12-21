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

	t || error();
	f && error();

	if (t || error()) { }
	if (f && error()) { }

	tmp = t || error();
	tmp = f && error();

	tmp = (t || error()) ? (t || error()) : 0;
	tmp = (f && error()) ? 1 : (f && error());

	if (t > (f && error())) { }
	if (f < (t || error())) { }
	
	tmp = t > (f && error());
	tmp = f < (t || error());

	if (!!!(t || error())) { }
	if (!!!(f && error())) { }

	return ({
		int __t = t;
		int __f = f;
		(f && error()) ? 1 : (f && error());
	} );
}
