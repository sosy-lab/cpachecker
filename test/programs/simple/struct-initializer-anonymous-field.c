struct S1 {
    signed : 0;
    signed f : 1;
};

int main (void)
{
    struct S1 s = {-1};
    if (s.f)
        ERROR: goto ERROR;
    return 0;
}
