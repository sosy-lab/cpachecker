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
int __return_244;
int __return_286;
int __return_346;
int __return_380;
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
if (((int)P_1_i) == 1)
{
P_1_st = 0;
goto label_164;
}
else 
{
P_1_st = 2;
label_164:; 
if (((int)P_2_i) == 1)
{
P_2_st = 0;
goto label_176;
}
else 
{
P_2_st = 2;
label_176:; 
if (((int)C_1_i) == 1)
{
C_1_st = 0;
goto label_188;
}
else 
{
C_1_st = 2;
label_188:; 
}
{
}
{
int tmp ;
int tmp___0 ;
int tmp___1 ;
{
int __retres1 ;
if (((int)P_1_pc) == 1)
{
if (((int)P_1_ev) == 1)
{
__retres1 = 1;
goto label_238;
}
else 
{
goto label_226;
}
}
else 
{
label_226:; 
__retres1 = 0;
label_238:; 
 __return_244 = __retres1;
}
tmp = __return_244;
if (tmp == 0)
{
goto label_254;
}
else 
{
P_1_st = 0;
label_254:; 
{
int __retres1 ;
if (((int)P_2_pc) == 1)
{
if (((int)P_2_ev) == 1)
{
__retres1 = 1;
goto label_280;
}
else 
{
goto label_268;
}
}
else 
{
label_268:; 
__retres1 = 0;
label_280:; 
 __return_286 = __retres1;
}
tmp___0 = __return_286;
if (tmp___0 == 0)
{
goto label_296;
}
else 
{
P_2_st = 0;
label_296:; 
{
int __retres1 ;
if (((int)C_1_pc) == 1)
{
if (((int)e) == 1)
{
__retres1 = 1;
goto label_336;
}
else 
{
goto label_310;
}
}
else 
{
label_310:; 
if (((int)C_1_pc) == 2)
{
if (((int)C_1_ev) == 1)
{
__retres1 = 1;
goto label_336;
}
else 
{
goto label_324;
}
}
else 
{
label_324:; 
__retres1 = 0;
label_336:; 
 __return_346 = __retres1;
}
tmp___1 = __return_346;
if (tmp___1 == 0)
{
goto label_356;
}
else 
{
C_1_st = 0;
label_356:; 
}
{
}
}
__retres2 = 0;
 __return_380 = __retres2;
return 1;
}
}
}
}
}
}
}
}
}
}
}
