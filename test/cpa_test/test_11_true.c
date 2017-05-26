void
func0(void)
{
    return;
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
    st->fptr = func0;
    if ((unsigned long)st->fptr == (unsigned long) (void (*)(void))0)
    {
        st->fptr();
ERROR: goto ERROR;
    }
    else
    {
    }

    return 0;
}

