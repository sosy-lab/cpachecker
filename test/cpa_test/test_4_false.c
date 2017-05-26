struct str1
{
    int a;
    int (*fptr)(int a);
};

struct str2
{
    int a;
    void (*fptr)(void);
};

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

int
func(int a)
{
    struct str2 st;
    st.a = a;

    if (st.a < 1)
        st.fptr = func0;
    else
        st.fptr = func1;

    st.fptr();
}

int
main(int argc, char **argv)
{
    struct str1 st;
    st.a = 2;
    st.fptr = func;

    st.fptr(st.a);

    return 0;
}

