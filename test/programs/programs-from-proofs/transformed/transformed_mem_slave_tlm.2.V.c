extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void error(void);
int m_run_st  ;
int m_run_i  ;
int m_run_pc  ;
int s_memory0  ;
int s_memory1  ;
int s_run_st  ;
int s_run_i  ;
int s_run_pc  ;
int c_m_lock  ;
int c_m_ev  ;
int c_req_type  ;
int c_req_a  ;
int c_req_d  ;
int c_rsp_type  ;
int c_rsp_status  ;
int c_rsp_d  ;
int c_empty_req  ;
int c_empty_rsp  ;
int c_read_req_ev  ;
int c_write_req_ev  ;
int c_read_rsp_ev  ;
int c_write_rsp_ev  ;
static int d_t  ;
static int a_t  ;
static int req_t_type  ;
static int req_t_a  ;
static int req_t_d  ;
static int rsp_t_type  ;
static int rsp_t_status  ;
static int rsp_t_d  ;
static int req_tt_type  ;
static int req_tt_a  ;
static int req_tt_d  ;
static int rsp_tt_type  ;
static int rsp_tt_status  ;
static int rsp_tt_d  ;
int s_memory_read(int i);
void s_memory_write(int i, int v);
void m_run(void);
static int req_t_type___0  ;
static int req_t_a___0  ;
static int req_t_d___0  ;
static int rsp_t_type___0  ;
static int rsp_t_status___0  ;
static int rsp_t_d___0  ;
void s_run(void);
void eval(void);
void start_simulation(void);
int main(void);
int __return_39703;
int __return_40292;
int __return_40885;
int main()
{
int __retres1 ;
c_m_lock = 0;
c_m_ev = 2;
m_run_i = 1;
m_run_pc = 0;
s_run_i = 1;
s_run_pc = 0;
c_empty_req = 1;
c_empty_rsp = 1;
c_read_req_ev = 2;
c_write_req_ev = 2;
c_read_rsp_ev = 2;
c_write_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 2;
{
int kernel_st ;
kernel_st = 0;
m_run_st = 0;
s_run_st = 0;
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_37749:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_37749;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_38229:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
goto label_38229;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
s_run_st = 0;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_38668:; 
label_38671:; 
tmp___0 = __VERIFIER_nondet_int();
label_38427:; 
if (tmp___0 == 0)
{
goto label_38668;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_1 = req_a;
int __tmp_2 = req_d;
int i = __tmp_1;
int v = __tmp_2;
s_memory0 = v;
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_38153;
}
}
}
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
a = 0;
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_37935:; 
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_37935;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_3 = req_a;
int __tmp_4 = req_d;
int i = __tmp_3;
int v = __tmp_4;
s_memory0 = v;
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_38153:; 
label_39002:; 
label_40792:; 
label_40795:; 
label_40798:; 
label_40801:; 
label_40804:; 
label_40807:; 
tmp = __VERIFIER_nondet_int();
label_38687:; 
if (tmp == 0)
{
goto label_39002;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
c_m_ev = 2;
a = a + 1;
req_type = 1;
req_a = a;
req_d = a + 50;
c_m_lock = 1;
c_req_type = req_type;
c_req_a = req_a;
c_req_d = req_d;
c_empty_req = 0;
c_write_req_ev = 1;
s_run_st = 0;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 3;
req_t_type = req_type;
req_t_a = req_a;
req_t_d = req_d;
rsp_t_type = rsp_type;
rsp_t_status = rsp_status;
rsp_t_d = rsp_d;
d_t = d;
a_t = a;
}
label_40784:; 
label_40787:; 
tmp___0 = __VERIFIER_nondet_int();
label_39004:; 
if (tmp___0 == 0)
{
goto label_40784;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_5 = req_a;
int __tmp_6 = req_d;
int i = __tmp_5;
int v = __tmp_6;
s_memory1 = v;
}
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_39575:; 
label_40750:; 
label_40753:; 
label_40756:; 
label_40759:; 
label_40762:; 
label_40765:; 
tmp = __VERIFIER_nondet_int();
label_39248:; 
if (tmp == 0)
{
goto label_39575;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type = req_t_type;
req_a = req_t_a;
req_d = req_t_d;
rsp_type = rsp_t_type;
rsp_status = rsp_t_status;
rsp_d = rsp_t_d;
d = d_t;
a = a_t;
rsp_type = c_rsp_type;
rsp_status = c_rsp_status;
rsp_d = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
c_m_ev = 2;
a = a + 1;
a = 0;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
s_run_st = 0;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_40742:; 
label_40745:; 
tmp___0 = __VERIFIER_nondet_int();
label_39577:; 
if (tmp___0 == 0)
{
goto label_40742;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_7 = req_a;
int i = __tmp_7;
int x;
x = s_memory0;
 __return_39703 = x;
}
rsp_d = __return_39703;
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_40162:; 
label_40708:; 
label_40711:; 
label_40714:; 
label_40717:; 
label_40720:; 
label_40723:; 
tmp = __VERIFIER_nondet_int();
label_39833:; 
if (tmp == 0)
{
goto label_40162;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
c_m_ev = 2;
a = a + 1;
req_type___0 = 0;
req_a___0 = a;
c_m_lock = 1;
c_req_type = req_type___0;
c_req_a = req_a___0;
c_req_d = req_d___0;
c_empty_req = 0;
c_write_req_ev = 1;
s_run_st = 0;
c_write_req_ev = 2;
m_run_st = 2;
m_run_pc = 6;
req_tt_type = req_type___0;
req_tt_a = req_a___0;
req_tt_d = req_d___0;
rsp_tt_type = rsp_type___0;
rsp_tt_status = rsp_status___0;
rsp_tt_d = rsp_d___0;
d_t = d;
a_t = a;
}
label_40700:; 
label_40703:; 
tmp___0 = __VERIFIER_nondet_int();
label_40164:; 
if (tmp___0 == 0)
{
goto label_40700;
}
else 
{
s_run_st = 1;
{
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int dummy ;
req_type = req_t_type___0;
req_a = req_t_a___0;
req_d = req_t_d___0;
rsp_type = rsp_t_type___0;
rsp_status = rsp_t_status___0;
rsp_d = rsp_t_d___0;
req_type = c_req_type;
req_a = c_req_a;
req_d = c_req_d;
c_empty_req = 1;
c_read_req_ev = 1;
c_read_req_ev = 2;
rsp_type = req_type;
{
int __tmp_8 = req_a;
int i = __tmp_8;
int x;
x = s_memory1;
 __return_40292 = x;
}
rsp_d = __return_40292;
rsp_status = 1;
c_rsp_type = rsp_type;
c_rsp_status = rsp_status;
c_rsp_d = rsp_d;
c_empty_rsp = 0;
c_write_rsp_ev = 1;
m_run_st = 0;
c_write_rsp_ev = 2;
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_40643:; 
label_40666:; 
label_40669:; 
label_40672:; 
label_40675:; 
label_40678:; 
label_40681:; 
tmp = __VERIFIER_nondet_int();
label_40422:; 
if (tmp == 0)
{
goto label_40643;
}
else 
{
m_run_st = 1;
{
int d ;
int a ;
int req_type ;
int req_a ;
int req_d ;
int rsp_type ;
int rsp_status ;
int rsp_d ;
int req_type___0 ;
int req_a___0 ;
int req_d___0 ;
int rsp_type___0 ;
int rsp_status___0 ;
int rsp_d___0 ;
req_type___0 = req_tt_type;
req_a___0 = req_tt_a;
req_d___0 = req_tt_d;
rsp_type___0 = rsp_tt_type;
rsp_status___0 = rsp_tt_status;
rsp_d___0 = rsp_tt_d;
d = d_t;
a = a_t;
rsp_type___0 = c_rsp_type;
rsp_status___0 = c_rsp_status;
rsp_d___0 = c_rsp_d;
c_empty_rsp = 1;
c_read_rsp_ev = 1;
c_read_rsp_ev = 2;
c_m_lock = 0;
c_m_ev = 1;
c_m_ev = 2;
a = a + 1;
}
}
kernel_st = 2;
kernel_st = 3;
}
__retres1 = 0;
 __return_40885 = __retres1;
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
