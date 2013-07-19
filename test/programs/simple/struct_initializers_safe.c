int main() {
	struct empty {
	} empty = { };

	struct u {
		struct v {
			int j;
		} v;
	} u = { -1 };
	struct s {
		struct u u;
		struct t {
			int e;
			int f;
			int g;
			int h;
		} t;
		int i;
		struct w {
			int k;
			int l;
		} w;
		int m;
	} s = { u.v, 1, .t.g = 2, 3, 4, 5 };
	
	if (s.u.v.j != -1) goto ERROR;
	if (s.t.e   !=  1) goto ERROR;
	if (s.t.f   !=  0) goto ERROR;
	if (s.t.g   !=  2) goto ERROR;
	if (s.t.h   !=  3) goto ERROR;
	if (s.i     !=  4) goto ERROR;
	if (s.w.k   !=  5) goto ERROR;
	if (s.w.l   !=  0) goto ERROR;
	if (s.m     !=  0) goto ERROR;

	printf("s.u.v.j = %d\n", s.u.v.j);
	printf("s.t.e = %d\n", s.t.e);
	printf("s.t.f = %d\n", s.t.f);
	printf("s.t.g = %d\n", s.t.g);
	printf("s.t.h = %d\n", s.t.h);
	printf("s.i   = %d\n", s.i);
	printf("s.w.k = %d\n", s.w.k);
	printf("s.w.l = %d\n", s.w.l);
	printf("s.m   = %d\n", s.m);

	struct {
		int i;
		int a[3];
	} r = { 1, 2, 3 };

	if (r.i    != 1) goto ERROR;
	if (r.a[0] != 2) goto ERROR;
	if (r.a[1] != 3) goto ERROR;

	return 0;
ERROR:
	return 1;
}
