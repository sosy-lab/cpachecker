# 1 "character_literals1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "character_literals1/main.c"




int some_array__LINE__[(sizeof('a')==sizeof(int)) ? 1 : -1];;

int some_array__LINE__[('\n' == 10) ? 1 : -1];;
int some_array__LINE__[('\0' == 0) ? 1 : -1];;
int some_array__LINE__[('\1' == 1) ? 1 : -1];;
int some_array__LINE__[('\144' == 100) ? 1 : -1];;
int some_array__LINE__[('\xff' == (char)0xff) ? 1 : -1];;



int some_array__LINE__[(L'\xff'==255) ? 1 : -1];;
int some_array__LINE__[(L'a'=='a') ? 1 : -1];;


int some_array__LINE__[(L'\x12345678'==0x12345678L) ? 1 : -1];;


int main()
{
}
