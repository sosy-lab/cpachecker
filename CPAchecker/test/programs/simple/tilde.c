void check(int cond) {
    if (!cond) {
        ERROR:
        goto ERROR;
    }
}

void main() {
   
    check(((char)-1)==~((char)0));
    check(((char)-6)==~((char)5));
    check(((char)-100)==~((char)99));
    check(((char)-128)==~((char)127));
    
    check(((unsigned char)255)!=~((unsigned char)0));
    check(((int)-1)==~((unsigned char)0));
    check(((unsigned char)250)!=~((unsigned char)5));
    check(((int)-6)==~((unsigned char)5));
    check(((unsigned char)156)!=~((unsigned char)99));
    check(((int)-100)==~((unsigned char)99));
    check(((unsigned char)128)!=~((unsigned char)127));
    check(((int)-128)==~((unsigned char)127));

    check(((short)-1)==~((short)0));
    check(((short)-6)==~((short)5));
    check(((short)-32768)==~((short)32767));
    
    check(((unsigned short)65535)!=~((unsigned short)0));
    check(((int)-1)==~((unsigned short)0));
    check(((unsigned short)65530)!=~((unsigned short)5));
    check(((int)-6)==~((unsigned short)5));
    check(((unsigned short)32768)!=~((unsigned short)32767));
    check(((int)-32768)==~((unsigned short)32767));
    
    check(((long long)-1)==~((long long)0));
    check(((long long)-6)==~((long long)5));
    check(((long long)-9223372036854775801LL)==~((long long)9223372036854775800LL));
    check(((long long)-9223372036854775808ULL)==~((long long)9223372036854775807LL));

    check(((unsigned long long)18446744073709551615ULL)==~((unsigned long long)0));
    check(((unsigned long long)18446744073709551610ULL)==~((unsigned long long)5));
    check(((unsigned long long)9223372036854775808ULL)==~((unsigned long long)9223372036854775807ULL));
    
}
