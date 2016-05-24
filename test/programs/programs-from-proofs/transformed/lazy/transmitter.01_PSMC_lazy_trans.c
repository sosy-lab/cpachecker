extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_pc  =    0;
int t1_pc  =    0;
int m_st  ;
int t1_st  ;
int m_i  ;
int t1_i  ;
int M_E  =    2;
int T1_E  =    2;
int E_1  =    2;
int is_master_triggered(void) ;
int is_transmit1_triggered(void) ;
void immediate_notify(void) ;
void master(void);
void transmit1(void);
void update_channels(void);
void init_threads(void);
int exists_runnable_thread(void);
void eval(void);
void fire_delta_events(void);
void reset_delta_events(void);
void activate_threads(void);
void fire_time_events(void);
void reset_time_events(void);
void init_model(void);
int stop_simulation(void);
void start_simulation(void);
int main(void);
int __return_2750;
int __return_2766;
int __return_3019;
int __return_4358;
int __return_4374;
int __return_4418;
int __return_4447;
int __return_4463;
int __return_4513;
int __return_4524;
int __return_4525;
int __return_4657;
int __return_4587;
int __return_4543;
int __return_4554;
int __return_4555;
int main()
{
int __retres1 ;
{
m_i = 1;
t1_i = 1;
}
{
int kernel_st ;
int tmp ;
int tmp___0 ;
kernel_st = 0;
{
}
{
m_st = 0;
t1_st = 0;
}
{
if (M_E < 1)
{
M_E = 1;
goto label_2728;
}
else 
{
label_2728:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_2750 = __retres1;
}
tmp = __return_2750;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_2766 = __retres1;
}
tmp___0 = __return_2766;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_2786;
}
else 
{
label_2786:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_2792;
}
else 
{
label_2792:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_2798;
}
else 
{
label_2798:; 
}
kernel_st = 1;
{
int tmp ;
label_3008:; 
{
int __retres1 ;
__retres1 = 1;
 __return_3019 = __retres1;
}
tmp = __return_3019;
}
label_4270:; 
kernel_st = 2;
{
}
kernel_st = 3;
{
if (M_E < 1)
{
M_E = 1;
goto label_4336;
}
else 
{
label_4336:; 
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_4358 = __retres1;
}
tmp = __return_4358;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_4374 = __retres1;
}
tmp___0 = __return_4374;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_4394;
}
else 
{
label_4394:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_4400;
}
else 
{
label_4400:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_4406;
}
else 
{
label_4406:; 
}
{
int __retres1 ;
__retres1 = 1;
 __return_4418 = __retres1;
}
tmp = __return_4418;
if (tmp < 1)
{
kernel_st = 4;
{
M_E = 1;
}
{
int tmp ;
int tmp___0 ;
{
int __retres1 ;
__retres1 = 0;
 __return_4447 = __retres1;
}
tmp = __return_4447;
m_st = 0;
{
int __retres1 ;
__retres1 = 0;
 __return_4463 = __retres1;
}
tmp___0 = __return_4463;
t1_st = 0;
}
{
if (M_E < 2)
{
M_E = 2;
goto label_4483;
}
else 
{
label_4483:; 
if (T1_E == 1)
{
T1_E = 2;
goto label_4489;
}
else 
{
label_4489:; 
if (E_1 == 1)
{
E_1 = 2;
goto label_4495;
}
else 
{
label_4495:; 
}
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_4513 = __retres1;
}
tmp = __return_4513;
if (tmp < 1)
{
__retres2 = 0;
 __return_4524 = __retres2;
}
else 
{
__retres2 = 1;
 __return_4525 = __retres2;
}
tmp___0 = __return_4524;
label_4529:; 
tmp___0 = __return_4525;
label_4527:; 
if (tmp___0 < 1)
{
}
else 
{
kernel_st = 1;
{
int tmp ;
{
int __retres1 ;
__retres1 = 1;
 __return_4587 = __retres1;
}
tmp = __return_4587;
}
goto label_4270;
}
label_4652:; 
__retres1 = 0;
 __return_4657 = __retres1;
return 1;
}
goto label_4652;
}
}
}
}
else 
{
{
int tmp ;
int __retres2 ;
{
int __retres1 ;
__retres1 = 1;
 __return_4543 = __retres1;
}
tmp = __return_4543;
if (tmp < 1)
{
__retres2 = 0;
 __return_4554 = __retres2;
}
else 
{
__retres2 = 1;
 __return_4555 = __retres2;
}
tmp___0 = __return_4554;
goto label_4529;
tmp___0 = __return_4555;
goto label_4527;
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
}
