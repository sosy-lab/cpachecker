void
func0(void)
{
    return;
}

void
func1(void)
{
    ERROR: goto ERROR;
}

struct str
{
    int a;
    void (*fptr)(void);
};

int
main(int argc, char **argv)
{
    struct str *st;
    int a = 0;

    if (a < 1)
        st->fptr = func0;
    else
        st->fptr = func1;

    st->fptr();

    return 0;
}

