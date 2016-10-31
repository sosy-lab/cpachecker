int main() {
	int i, j, *p, *q, **r, **s;
	p = &i;
	r = &q;
	*r = &i;
	s = r;
	r = &p;
}

