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
int __return_151427;
int __return_151428;
int __return_151426;
int __return_147455;
int __return_148351;
int __return_150222;
int __return_133876;
int __return_151421;
int __return_138956;
int __return_140660;
int __return_140866;
int __return_142739;
int __return_132037;
int __return_151422;
int __return_144032;
int __return_145233;
int __return_135201;
int __return_151424;
int __return_146583;
int __return_137072;
int __return_151425;
int __return_151423;
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
if (!(((int)m_run_i) == 1))
{
m_run_st = 2;
if (!(((int)s_run_i) == 1))
{
s_run_st = 2;
label_131605:; 
label_138518:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_138527:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_138544:; 
goto label_138527;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_138544;
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
label_138582:; 
label_138650:; 
label_138651:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
goto label_138651;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_138650;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_138582;
}
}
}
label_138662:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_138785:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_138802:; 
goto label_138785;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_138849;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_138849:; 
goto label_138802;
}
}
}
goto label_138662;
}
}
__retres1 = 0;
 __return_151428 = __retres1;
return 1;
}
}
}
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_138518;
}
__retres1 = 0;
 __return_151427 = __retres1;
return 1;
}
}
else 
{
s_run_st = 0;
goto label_131605;
}
}
else 
{
m_run_st = 0;
if (!(((int)s_run_i) == 1))
{
s_run_st = 2;
label_131603:; 
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_131845:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
label_131951:; 
goto label_131845;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_131951;
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
label_132209:; 
label_132601:; 
label_132602:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_132602;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_132601;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_132209;
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
label_133748:; 
label_133754:; 
if (!(((int)s_run_st) == 0))
{
label_133766:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_133754;
}
label_134032:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_147328:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
label_147341:; 
if (!(((int)s_run_st) == 0))
{
label_147345:; 
goto label_147328;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_147345;
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
if (((int)req_type) == 0)
{
{
int __tmp_1 = req_a;
int i = __tmp_1;
int x;
if (i == 0)
{
x = s_memory0;
goto label_147450;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_147450;
}
else 
{
{
__VERIFIER_error();
}
label_147450:; 
 __return_147455 = x;
}
rsp_d = __return_147455;
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
goto label_147596;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_2 = req_a;
int __tmp_3 = req_d;
int i = __tmp_2;
int v = __tmp_3;
if (!(i == 0))
{
if (!(i == 1))
{
{
__VERIFIER_error();
}
label_147427:; 
}
else 
{
s_memory1 = v;
goto label_147427;
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
goto label_147596;
}
else 
{
s_memory0 = v;
goto label_147427;
}
}
}
else 
{
rsp_status = 0;
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
label_147596:; 
}
label_147598:; 
label_148025:; 
label_148026:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_148026;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_148025;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_147598;
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
if (a < 2)
{
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
else 
{
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
goto label_147341;
label_148265:; 
if (!(((int)s_run_st) == 0))
{
label_148274:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_148265;
}
goto label_138250;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_148274;
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
int __tmp_4 = req_a;
int i = __tmp_4;
int x;
x = s_memory0;
 __return_148351 = x;
}
rsp_d = __return_148351;
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
label_149081:; 
label_149514:; 
label_149515:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_149515;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_149514;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_149081;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_149628;
}
else 
{
{
__VERIFIER_error();
}
label_149628:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
label_150137:; 
label_150139:; 
if (!(((int)s_run_st) == 0))
{
label_150143:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_150139;
}
goto label_138222;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_150143;
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
int i = __tmp_5;
int x;
if (i == 1)
{
x = s_memory1;
goto label_150219;
}
else 
{
{
__VERIFIER_error();
}
label_150219:; 
 __return_150222 = x;
}
rsp_d = __return_150222;
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
label_150766:; 
label_151056:; 
label_151057:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_151057;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_151056;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_150766;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_151170;
}
else 
{
{
__VERIFIER_error();
}
label_151170:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
goto label_150137;
label_151246:; 
if (!(((int)s_run_st) == 0))
{
label_151254:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_151246;
}
goto label_138208;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_151346;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_151346:; 
goto label_151254;
}
}
}
}
}
}
}
}
label_149705:; 
if (!(((int)s_run_st) == 0))
{
label_149717:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_149705;
}
goto label_138236;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_149947;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_149947:; 
goto label_149717;
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
}
}
goto label_134032;
}
}
__retres1 = 0;
 __return_151426 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_133766;
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
if (((int)req_type) == 0)
{
{
int __tmp_6 = req_a;
int i = __tmp_6;
int x;
if (i == 0)
{
x = s_memory0;
goto label_133871;
}
else 
{
if (i == 1)
{
x = s_memory1;
goto label_133871;
}
else 
{
{
__VERIFIER_error();
}
label_133871:; 
 __return_133876 = x;
}
rsp_d = __return_133876;
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
goto label_134017;
}
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_7 = req_a;
int __tmp_8 = req_d;
int i = __tmp_7;
int v = __tmp_8;
if (!(i == 0))
{
if (!(i == 1))
{
{
__VERIFIER_error();
}
label_133848:; 
}
else 
{
s_memory1 = v;
goto label_133848;
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
goto label_134017;
}
else 
{
s_memory0 = v;
goto label_133848;
}
}
}
else 
{
rsp_status = 0;
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
label_134017:; 
}
goto label_134454;
}
}
}
}
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
label_131944:; 
if (!(((int)s_run_st) == 0))
{
label_131952:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_131944;
}
label_138264:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_138861:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_138878:; 
goto label_138861;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_138878;
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
if (((int)req_type) == 0)
{
{
int __tmp_9 = req_a;
int i = __tmp_9;
int x;
x = s_memory0;
 __return_138956 = x;
}
rsp_d = __return_138956;
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
label_139096:; 
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_15 = req_a;
int __tmp_16 = req_d;
int i = __tmp_15;
int v = __tmp_16;
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
goto label_139096;
}
else 
{
rsp_status = 0;
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
goto label_139096;
}
}
label_139615:; 
label_140290:; 
label_140291:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_140291;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_140290;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_139615;
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
if (a < 2)
{
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
else 
{
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
label_140531:; 
if (!(((int)s_run_st) == 0))
{
label_140543:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_140531;
}
goto label_134032;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_140543;
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
if (((int)req_type) == 0)
{
{
int __tmp_10 = req_a;
int i = __tmp_10;
int x;
if (i == 1)
{
x = s_memory1;
goto label_140657;
}
else 
{
{
__VERIFIER_error();
}
label_140657:; 
 __return_140660 = x;
}
rsp_d = __return_140660;
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
goto label_140801;
}
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_11 = req_a;
int __tmp_12 = req_d;
int i = __tmp_11;
int v = __tmp_12;
if (!(i == 1))
{
{
__VERIFIER_error();
}
label_140637:; 
}
else 
{
s_memory1 = v;
goto label_140637;
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
goto label_140801;
}
}
else 
{
rsp_status = 0;
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
label_140801:; 
}
goto label_139615;
}
}
}
}
label_140530:; 
if (!(((int)s_run_st) == 0))
{
label_140542:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_140530;
}
goto label_138250;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_140542;
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
int __tmp_13 = req_a;
int i = __tmp_13;
int x;
x = s_memory0;
 __return_140866 = x;
}
rsp_d = __return_140866;
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
label_141598:; 
label_142031:; 
label_142032:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_142032;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_142031;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_141598;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_142145;
}
else 
{
{
__VERIFIER_error();
}
label_142145:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
label_142654:; 
label_142656:; 
if (!(((int)s_run_st) == 0))
{
label_142660:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_142656;
}
goto label_138222;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_142660;
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
int __tmp_14 = req_a;
int i = __tmp_14;
int x;
if (i == 1)
{
x = s_memory1;
goto label_142736;
}
else 
{
{
__VERIFIER_error();
}
label_142736:; 
 __return_142739 = x;
}
rsp_d = __return_142739;
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
label_143283:; 
label_143573:; 
label_143574:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_143574;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_143573;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_143283;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_143687;
}
else 
{
{
__VERIFIER_error();
}
label_143687:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
goto label_142654;
label_143763:; 
if (!(((int)s_run_st) == 0))
{
label_143771:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_143763;
}
goto label_138208;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_143863;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_143863:; 
goto label_143771;
}
}
}
}
}
}
}
}
label_142222:; 
if (!(((int)s_run_st) == 0))
{
label_142234:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_142222;
}
goto label_138236;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_142464;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_142464:; 
goto label_142234;
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
}
goto label_138264;
}
}
__retres1 = 0;
 __return_151421 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_131952;
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
if (((int)req_type) == 0)
{
{
int __tmp_17 = req_a;
int i = __tmp_17;
int x;
x = s_memory0;
 __return_132037 = x;
}
rsp_d = __return_132037;
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
label_132177:; 
}
else 
{
if (((int)req_type) == 1)
{
{
int __tmp_23 = req_a;
int __tmp_24 = req_d;
int i = __tmp_23;
int v = __tmp_24;
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
goto label_132177;
}
else 
{
rsp_status = 0;
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
goto label_132177;
}
}
label_134454:; 
label_134877:; 
label_134878:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_134878;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_134877;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_134454;
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
if (a < 2)
{
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
else 
{
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
goto label_133748;
label_135116:; 
if (!(((int)s_run_st) == 0))
{
label_135124:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_135116;
}
label_138250:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_143945:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_143962:; 
goto label_143945;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_143962;
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
int __tmp_18 = req_a;
int i = __tmp_18;
int x;
x = s_memory0;
 __return_144032 = x;
}
rsp_d = __return_144032;
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
label_144090:; 
label_144525:; 
label_144526:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_144526;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_144525;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_144090;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_144639;
}
else 
{
{
__VERIFIER_error();
}
label_144639:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
label_145148:; 
label_145150:; 
if (!(((int)s_run_st) == 0))
{
label_145154:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_145150;
}
goto label_138222;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_145154;
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
int __tmp_19 = req_a;
int i = __tmp_19;
int x;
if (i == 1)
{
x = s_memory1;
goto label_145230;
}
else 
{
{
__VERIFIER_error();
}
label_145230:; 
 __return_145233 = x;
}
rsp_d = __return_145233;
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
label_145777:; 
label_146067:; 
label_146068:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_146068;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_146067;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_145777;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_146181;
}
else 
{
{
__VERIFIER_error();
}
label_146181:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
goto label_145148;
label_146257:; 
if (!(((int)s_run_st) == 0))
{
label_146265:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_146257;
}
goto label_138208;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_146357;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_146357:; 
goto label_146265;
}
}
}
}
}
}
}
}
label_144716:; 
if (!(((int)s_run_st) == 0))
{
label_144728:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_144716;
}
goto label_138236;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_144958;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_144958:; 
goto label_144728;
}
}
}
}
}
}
}
}
goto label_138250;
}
}
__retres1 = 0;
 __return_151422 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_135124;
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
int __tmp_20 = req_a;
int i = __tmp_20;
int x;
x = s_memory0;
 __return_135201 = x;
}
rsp_d = __return_135201;
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
label_135931:; 
label_136364:; 
label_136365:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_136365;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_136364;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_135931;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_136478;
}
else 
{
{
__VERIFIER_error();
}
label_136478:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
label_136987:; 
label_136989:; 
if (!(((int)s_run_st) == 0))
{
label_136993:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_136989;
}
label_138222:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_146487:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
label_146500:; 
if (!(((int)s_run_st) == 0))
{
label_146504:; 
goto label_146487;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_146504;
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
int __tmp_21 = req_a;
int i = __tmp_21;
int x;
if (i == 1)
{
x = s_memory1;
goto label_146580;
}
else 
{
{
__VERIFIER_error();
}
label_146580:; 
 __return_146583 = x;
}
rsp_d = __return_146583;
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
label_146640:; 
label_146934:; 
label_146935:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_146935;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_146934;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_146640;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_147048;
}
else 
{
{
__VERIFIER_error();
}
label_147048:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
goto label_146500;
label_147125:; 
if (!(((int)s_run_st) == 0))
{
label_147134:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_147125;
}
goto label_138208;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_147226;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_147226:; 
goto label_147134;
}
}
}
}
}
}
}
}
}
goto label_138222;
}
}
__retres1 = 0;
 __return_151424 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_136993;
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
int __tmp_22 = req_a;
int i = __tmp_22;
int x;
if (i == 1)
{
x = s_memory1;
goto label_137069;
}
else 
{
{
__VERIFIER_error();
}
label_137069:; 
 __return_137072 = x;
}
rsp_d = __return_137072;
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
label_137616:; 
label_137906:; 
label_137907:; 
tmp = __VERIFIER_nondet_int();
if (tmp == 0)
{
if (!(((int)s_run_st) == 0))
{
goto label_137907;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_137906;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
goto label_137616;
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
if ((req_a___0 + 50) == rsp_d___0)
{
goto label_138020;
}
else 
{
{
__VERIFIER_error();
}
label_138020:; 
a = a + 1;
if (a < 2)
{
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
else 
{
}
goto label_136987;
label_138096:; 
if (!(((int)s_run_st) == 0))
{
label_138104:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_138096;
}
label_138208:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_147252:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_147269:; 
goto label_147252;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_147316;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_147316:; 
goto label_147269;
}
}
}
goto label_138208;
}
}
__retres1 = 0;
 __return_151425 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_138196;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_138196:; 
goto label_138104;
}
}
}
}
}
}
}
}
label_136555:; 
if (!(((int)s_run_st) == 0))
{
label_136567:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
goto label_136555;
}
label_138236:; 
kernel_st = 2;
kernel_st = 3;
if (!(((int)s_run_st) == 0))
{
}
else 
{
kernel_st = 1;
{
int tmp ;
int tmp___0 ;
label_146411:; 
if (!(((int)s_run_st) == 0))
{
}
else 
{
if (!(((int)s_run_st) == 0))
{
label_146428:; 
goto label_146411;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_146475;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_146475:; 
goto label_146428;
}
}
}
goto label_138236;
}
}
__retres1 = 0;
 __return_151423 = __retres1;
return 1;
}
else 
{
tmp___0 = __VERIFIER_nondet_int();
if (tmp___0 == 0)
{
goto label_136797;
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
s_run_st = 2;
s_run_pc = 2;
req_t_type___0 = req_type;
req_t_a___0 = req_a;
req_t_d___0 = req_d;
rsp_t_type___0 = rsp_type;
rsp_t_status___0 = rsp_status;
rsp_t_d___0 = rsp_d;
}
label_136797:; 
goto label_136567;
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
}
}
}
else 
{
s_run_st = 0;
goto label_131603;
}
}
}
}
