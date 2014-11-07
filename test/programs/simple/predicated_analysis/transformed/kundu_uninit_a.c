extern int __VERIFIER_nondet_int();
void error(void);
void immediate_notify(void) ;
int max_loop ;
int clk ;
int num ;
int i  ;
int e  ;
int timer ;
char data_0  ;
char data_1  ;
char read_data(int i___0 );
void write_data(int i___0 , char c );
int P_1_pc;
int P_1_st  ;
int P_1_i  ;
int P_1_ev  ;
void P_1(void);
int is_P_1_triggered(void);
int P_2_pc  ;
int P_2_st  ;
int P_2_i  ;
int P_2_ev  ;
void P_2(void);
int is_P_2_triggered(void);
int C_1_pc  ;
int C_1_st  ;
int C_1_i  ;
int C_1_ev  ;
int C_1_pr  ;
void C_1(void);
int is_C_1_triggered(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void fire_time_events(void);
void reset_time_events(void);
void activate_threads(void);
int stop_simulation(void);
void start_simulation(void);
void init_model(void);
int main(void);
int __return_57631;
int __return_57658;
int __return_57699;
int __return_61717;
int __return_61872;
int __return_62700;
int __return_62674;
int __return_61832;
int main()
{
int count ;
int __retres2 ;
num = 0;
i = 0;
clk = 0;
max_loop = 8;
e;
timer = 0;
P_1_pc = 0;
P_2_pc = 0;
C_1_pc = 0;
count = 0;
{
P_1_i = 1;
P_2_i = 1;
C_1_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
P_1_st = 0;
P_2_st = 0;
C_1_st = 0;
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
__retres1 = 0;
 __return_57631 = __retres1;
}
tmp = __return_57631;
if (tmp == 0)
{
label_57637:; 
{
int __retres1 ;
__retres1 = 0;
 __return_57658 = __retres1;
}
tmp___0 = __return_57658;
if (tmp___0 == 0)
{
label_57666:; 
{
int __retres1 ;
if (((int)C_1_pc) == 2)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
 __return_57699 = __retres1;
}
tmp___1 = __return_57699;
if (tmp___1 == 0)
{
label_57707:; 
}
else 
{
C_1_st = 0;
goto label_57707;
}
{
}
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
int tmp___2 ;
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_61717 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_61717;
tmp = 0;
tmp___0 = 0;
label_61750:; 
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
label_61766:; 
label_61770:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_61872 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_61872;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = 0;
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_61766;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
goto label_62640;
}
else 
{
C_1_st = 2;
}
goto label_62559;
}
}
}
else 
{
goto label_61770;
}
}
return 1;
}
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
if (num == 0)
{
timer = 1;
i = i + 1;
C_1_pc = 1;
C_1_st = 2;
}
else 
{
return 1;
}
label_62640:; 
label_62651:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_62700 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_62700;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
goto label_62651;
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
return 1;
}
}
else 
{
C_1_st = 2;
}
label_62559:; 
label_62645:; 
label_62649:; 
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_62674 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_62674;
if (tmp___2 == 0)
{
}
else 
{
if (((int)P_1_st) == 0)
{
tmp = 0;
if (((int)P_2_st) == 0)
{
tmp___0 = 0;
if (((int)C_1_st) == 0)
{
tmp___1 = __VERIFIER_nondet_int();
if (tmp___1 == 0)
{
goto label_62645;
}
else 
{
C_1_st = 1;
{
char c ;
if (i < max_loop)
{
return 1;
}
else 
{
C_1_st = 2;
}
goto label_62559;
}
}
}
else 
{
goto label_62649;
}
}
else 
{
return 1;
}
}
else 
{
return 1;
}
}
return 1;
}
}
}
}
else 
{
{
int __retres1 ;
if (((int)P_1_st) == 0)
{
__retres1 = 1;
 __return_61832 = __retres1;
}
else 
{
if (((int)C_1_st) == 0)
{
__retres1 = 1;
return 1;
}
else 
{
__retres1 = 0;
return 1;
}
}
tmp___2 = __return_61832;
if (tmp___2 == 0)
{
}
else 
{
tmp = 0;
tmp___0 = 0;
goto label_61750;
}
return 1;
}
}
}
}
}
}
else 
{
P_2_st = 0;
goto label_57666;
}
}
else 
{
P_1_st = 0;
goto label_57637;
}
}
}
}
