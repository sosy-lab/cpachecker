void
func0(void)
{
    return;
}

struct str0
{
    int b;
    void (*fptr)(void);
};

struct str
{
    int a;
    struct str0 *st0;
};

void true_func() {
}

void err_func() {
	ERROR: goto ERROR;
}

void
func1(struct str0 *st_in)
{
	st_in->fptr();
}

int g(void (*fn)(void)) {
    struct str0 *st_dop;
    st_dop->fptr = fn;
    func1(st_dop);
}

int
main(int argc, char **argv)
{
    int a = 3;
    void (*func_var)(void);
    if (a < 2) {
		func_var = &err_func;
	} else {
		func_var = &true_func;
	}
    g(func_var);
    return 0;
}
