int
func(int a)
{
    if (a < 1)
        return 0;

    ERROR: goto ERROR;

    return -1;
}

struct str
{
    int a;
    int (*fptr)(int a);
};

int
main(int argc, char **argv)
{
    struct str st;
    st.a = 0;
    st.fptr = func;

    st.fptr(st.a);

    return 0;
}

