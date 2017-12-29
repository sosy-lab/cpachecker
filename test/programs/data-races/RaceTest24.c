/* Test for creating recursion partitioning */

int a() {
    int a = 0;
    b();
}

int b() {
    int a = 0;
    e();
}

int c() {
    //int a = 0;
    a();
}

int d() {
    int a = 0;
    a();
    e();
}

int e() {
    int a = 0;
    h();
    i();
}

int f() {
    int a = 0;
    e();
}

int g() {
    int a = 0;
    c();
    e();
    j();
}

int h() {
    int a = 0;
    g();
}

int i() {
    int a = 0;
    e();
    k();
}

int j() {
    int a = 0;
    g();
}

int k() {
    int a = 0;
    l();
}

int l() {
    int a = 0;
    n();
}

int m() {
    int a = 0;
    n();
}

int n() {
    int a = 0;
    l();
    o();
}

int o() {
    int a = 0;
    l();
    k();
}

int ldv_main() {
	e();
}
