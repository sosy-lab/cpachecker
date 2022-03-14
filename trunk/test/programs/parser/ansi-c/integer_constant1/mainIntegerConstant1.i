# 1 "integer_constant1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "integer_constant1/main.c"



int some_array__LINE__[('\''==39) ? 1 : -1];;
int some_array__LINE__[(L'\''==39) ? 1 : -1];;

int some_array__LINE__[('\0'==0) ? 1 : -1];;
int some_array__LINE__[('\10'==8) ? 1 : -1];;
int some_array__LINE__[((signed char)'\xab'==(signed char)0xab) ? 1 : -1];;
int some_array__LINE__[(L'\xab'==0xab) ? 1 : -1];;
int some_array__LINE__[(L'\xabcd'==0xabcd) ? 1 : -1];;


int some_array__LINE__[('abcd'==('a'<<24)+('b'<<16)+('c'<<8)+'d') ? 1 : -1];;


int some_array__LINE__[(sizeof(1)==sizeof(int)) ? 1 : -1];;
int some_array__LINE__[(sizeof(1l)==sizeof(long int)) ? 1 : -1];;
int some_array__LINE__[(sizeof(1ll)==sizeof(long long int)) ? 1 : -1];;
int some_array__LINE__[(sizeof(0xaaaabbbbcccc)==sizeof(long long int)) ? 1 : -1];;
int some_array__LINE__[(sizeof('x')==sizeof(int)) ? 1 : -1];;



int some_array__LINE__[(0b101010==42) ? 1 : -1];;
int some_array__LINE__[(0B101010==42) ? 1 : -1];;
int some_array__LINE__[(sizeof(0B101010)==sizeof(int)) ? 1 : -1];;
int some_array__LINE__[(sizeof(0B101010LL)==sizeof(long long)) ? 1 : -1];;
int some_array__LINE__[(sizeof(0B101010)==sizeof(int)) ? 1 : -1];;
int some_array__LINE__[(0b10000000000000000000000000000000==2147483648) ? 1 : -1];;
int some_array__LINE__[(sizeof(0b10000000000000000000000000000000)==sizeof(int)) ? 1 : -1];;





int some_array__LINE__[(sizeof(L'x')==sizeof(int)) ? 1 : -1];;
int some_array__LINE__[(L'\xabcdabcd'==0xabcdabcd) ? 1 : -1];;
int some_array__LINE__[(L'\xfbcdabcd'==0xfbcdabcd) ? 1 : -1];;


int main()
{
}
