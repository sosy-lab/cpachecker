# 1 "Fixedbv2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Fixedbv2/main.c"
main() {
        float a;
        double b;

        a=1.25L;
        assert(a==1.25);

        b=1.250;
        assert(b==1.25);
}
