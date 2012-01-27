static char rcsid[] = "$Id: parser.c,v 1.27 2009/08/05 22:06:06 profw Exp $";
/* PGS parser frame */

/* Options that can be set via #define (all are normally unset):
 *
 * ZERTEILERTEST :
 *   - Set   : Es werden Testanweisungen erzeugt und auf die Datei
 *             Test geschrieben.
 *   - Unset : Es werden keine Testanweisungen erzeugt.
 * EINSETZEN :
 *   - Set   : Die Tabellenzugriffe sind in der Zerteilerschleife
 *             offen eingebaut.
 *   - Unset : Die Tabellenzugriffe sind nicht offen eingebaut.
 * MESSUNG :
 *   - Set   : Die verbrauchte Zeit und die Haeufigkeit verschiedener
 *             Aktionen werden gemessen und auf die Datei Test
 *             geschrieben. Aktionen innerhalb der Fehlerbehandlung
 *             werden nicht mitgezaehlt.
 *   - Unset : Es werden keine Leistungsdaten gemessen.
 * VERBESSERUNG :
 *   - Set   : Die Fehlerbehandlung versucht den Syntaxfehler nach
 *             einem heuristischen Verfahren mit mehreren Zeichen
 *             Vorschau zu verbessern. Falls das nicht gelingt, wird
 *             das Verfahren von J. ROEHRICH angewandt.
 *   - Unset : Die Fehlerbehandlung arbeitet nur mit dem Verfahren
 *             von J. ROEHRICH.
 */

#include <stdio.h>
#include <stdlib.h>
#include "err.h"
#include "gla.h"
#include "gsdescr.h"
#include "reparatur.h"

#ifdef MONITOR
#include "dapto_dapto.h"
#include "pgs_dapto.h"

extern int ntlen[], conc_prodmap[];
#endif

#ifdef RIGHTCOORD
POSITION rightpos;
#endif

#define FehlerDeskriptor(T, ZPos, ZG) \
	T_POS(*(ZG)) = *(ZPos); T_CODE(*(ZG)) = (T); T_ATTR(*(ZG)) = 0;

#if defined(__cplusplus) || defined(__STDC__)
extern void
StrukturAnknuepfung(unsigned PR, GRUNDSYMBOLDESKRIPTOR *ZAttributKeller);
#else
extern void StrukturAnknuepfung();
#endif

static int severity[] = {DEADLY, DEADLY, DEADLY, ERROR, DEADLY, NOTE};
static CONST char *text[] = {
	"",			/* 0 */
	"",			/* 1 */
	"Wrong parse tables",	/* 2 */
	"Parse stack overflow",	/* 3 */
	"",			/* 4 */
	"Syntax error",		/* 5 */
	"Symbol inserted",	/* 6 */
	"Symbol deleted",	/* 7 */
	"Parsing resumed here"};/* 8 */

#if defined(__cplusplus) || defined(__STDC__) 
static void
FehlerMeldung(int Klasse, int Schluessel, POSITION *ZPos, GRUNDSYMBOLDESKRIPTOR *ZG)
#else
static void
FehlerMeldung(Klasse, Schluessel, ZPos, ZG)
int Klasse, Schluessel; POSITION *ZPos; GRUNDSYMBOLDESKRIPTOR *ZG;
#endif
/* Klasse     : deadly error       = 1  repair          = 4           */
/*              limitation         = 2  information     = 5           */
/*              error              = 3                                */
/*                                                                    */
/* Schluessel : wrong parse tables = 2  symbol inserted = 6           */
/*              stack overflow     = 3  symbol deleted  = 7           */
/*              syntax error       = 5  restart point   = 8           */
/*                                                                    */
/* *ZPos      : error location                                        */
/*                                                                    */
/* *ZG        : token deleted or inserted by the error recovery       */
/*                                                                    */
/* when a syntax error occurs, the inserted or deleted tokens are     */
/* reported. The succession of tokens is directed by the calling      */
/* sequence of this procedure. Additionally, the position of the      */
/* next correct input token is reported.                              */
{
	if (Klasse != 4) message(severity[Klasse],text[Schluessel],0,ZPos);
}

#ifdef MESSUNG
extern  long  CPUTime();
#endif /* MESSUNG */

typedef unsigned char   K_TYP1;    /* 1 Byte  unsigned */
typedef unsigned short  K_TYP2;    /* 2 Bytes unsigned */
typedef unsigned short  M_TYP;     /* Komponententyp des Mengenfeldes */
#define M_BITS  16                 /* (Bits pro Byte) * sizeof(M_TYP) */
#define M_SHIFT 4                  /* ld(M_BITS) */
#define M_MASKE 017                /* Maske : (M_BITS - 1) */
#define M_DIV(x) (x >> M_SHIFT)    /* x DIV M_BITS */
#define M_MOD(x) (x & M_MASKE)     /* x MOD M_BITS */
#define INMENGE(m,x) (*(m + M_DIV(x)) >> M_MOD(x) & 01)
#define ZUMENGE(m,x) (*(m + M_DIV(x)) |= 01 << M_MOD(x))

#include "pgs_gen.h"

#ifdef ZERTEILERTEST
#define  LIEST            1
#define  REDUZIERT        2
#define  UEBERLIEST       3
#define  FUEGTEIN         4
#define  FEHLERSYMBOL     5
#define  FEHLERZUSTAND    6
#define  KELLERSYMBOL     7

#endif /* ZERTEILERTEST */
#define  UEBERSETZERFEHLER           1
#define  UEBERSETZERBESCHRAENKUNG    2
#define  SCHWERERFEHLER              3
#define  REPARATUR                   4
#define  KOMMENTAR                   5

#define  TABELLENFALSCH              2
#define  KELLERVOLL                  3
#define  SYNTAXFEHLER                5
#define  SYMBOLEINGEFUEGT            6
#define  SYMBOLGELOESCHT             7
#define  AUFSETZPUNKT                8

#define  STARTZUSTAND     1
#define  KEINZUSTAND      0

#define  KELLERENDE       150

#ifdef VERBESSERUNG
#define  MAXVORSCHAU      4  /* Pufferlaenge fuer Vorschau */
                             /* bei Fehlerbeh.             */

#endif /* VERBESSERUNG */
/**********************************************************************/
/*            Zustandsgroessen des Zerteilers                         */
/**********************************************************************/

typedef K_TYP2 *ZERTEILERKELLER;
#define NewZerteilerkeller \
  ((ZERTEILERKELLER)malloc((unsigned)((StackSize + 1) * sizeof(K_TYP2))))

typedef GRUNDSYMBOLDESKRIPTOR *ATTRIBUTKELLER;
#define NewAttributkeller \
  ((ATTRIBUTKELLER)malloc( \
    (unsigned)((StackSize + 1) * sizeof(GRUNDSYMBOLDESKRIPTOR))))

typedef
  enum
  {  BehandleFehler, Lies, Reduziere, LiesReduziere, LiesNT,
     LiesNTReduziere
  }  ZERTEILERAKTION;

#define  ANZAHLAKTIONEN  6

typedef
  struct
  { ZERTEILERAKTION  Aktion;
    union
    {  K_TYP2           FolgeZustand;
       K_TYP2           ProduktionsNr;
    }  V;
  }  TABELLENEINTRAG;


static  size_t			StackSize = KELLERENDE;
static  ZERTEILERKELLER		Keller, Kel = (ZERTEILERKELLER)0;
static  ATTRIBUTKELLER		AttributKeller = (ATTRIBUTKELLER)0;
static  size_t			Pegel;
static  TABELLENEINTRAG         Eintrag;
static  GRUNDSYMBOLDESKRIPTOR   GrundSym;
static  int                     gefunden;  
#ifdef VERBESSERUNG

/**********************************************************************/
/*            Vorschau der Fehlerbehandlung                           */
/**********************************************************************/

static  GRUNDSYMBOLDESKRIPTOR   Vorschau[MAXVORSCHAU + 1];
static  unsigned                VorschauAnfang = 1,
                                VorschauEnde   = 1;
static  int                     verbessert;
#endif /* VERBESSERUNG */
#ifdef MESSUNG

/**********************************************************************/
/*            Variablen zur Zeitmessung und zum Zaehlen von Aktionen  */
/**********************************************************************/

static  long                    StartZeit, StopZeit, 
                                ZerteilungsZeit;
static  long                    LeseZaehler = 0,
                                AktionsZaehler[ANZAHLAKTIONEN] =
                                {0, 0, 0, 0, 0, 0};

static  long                    DefaultZaehler = 0;

#endif /* MESSUNG */
#ifdef ZERTEILERTEST

/**********************************************************************/
/*            Variablen zur Kontrolle der Testausgabe                 */
/**********************************************************************/

#ifdef VERBESSERUNG
int                             KeineVerbesserung = 0;
#endif /* VERBESSERUNG */
int                             TabellenTest = 0,
                                ZerteilerTest = 0,
                                TestZerteiler = 0;
#endif /* ZERTEILERTEST */

#define Abbruch(Klasse, Schluessel, ZPos) \
	message(severity[Klasse], text[Schluessel], 0, ZPos)

static void
Expand()
{
  StackSize += KELLERENDE;

  if (Kel)
    Kel =
      (ZERTEILERKELLER)realloc(
        Kel, (unsigned)((StackSize + 1) * sizeof(K_TYP2)));

  Keller =
    (ZERTEILERKELLER)realloc(
      Keller, (unsigned)((StackSize + 1) * sizeof(K_TYP2)));

  AttributKeller =
    (ATTRIBUTKELLER)realloc(
      AttributKeller,
      (unsigned)((StackSize + 1) * sizeof(GRUNDSYMBOLDESKRIPTOR)));
}

#ifdef ZERTEILERTEST
#if defined(__cplusplus) || defined(__STDC__) 
static void
DruckeZerteilerZustand(K_TYP2 Z, int T, TABELLENEINTRAG *ZE)
#else
static void
DruckeZerteilerZustand(Z, T, ZE)
K_TYP2            Z;
int    T;
TABELLENEINTRAG  *ZE;
#endif
{
  printf("*ZT* Zustand =%4d Symbol =%4d", Z, T);
  switch(ZE->Aktion)
  {
    case Lies            : printf(" Lies            %4d\n", 
                                  ZE->V.FolgeZustand);
                           break;
    case LiesReduziere   : printf(" LiesReduziere   %4d\n", 
                                  ZE->V.ProduktionsNr);
                           break;
    case LiesNT          : printf(" LiesNT          %4d\n", 
                                  ZE->V.FolgeZustand);
                           break;
    case LiesNTReduziere : printf(" LiesNTReduziere %4d\n", 
                                  ZE->V.ProduktionsNr);
                           break;
    case Reduziere       : printf(" Reduziere       %4d\n", 
                                  ZE->V.ProduktionsNr);
                           break;
    case BehandleFehler  : printf(" BehandleFehler  ****\n");
                           break;
  }
}
#endif /* ZERTEILERTEST */


#if defined(__cplusplus) || defined(__STDC__) 
static void
TTabelle(K_TYP2 Z, int S, TABELLENEINTRAG *ZEintrag)
#else
static void
TTabelle(Z, S, ZEintrag)
K_TYP2            Z;
int    S;
TABELLENEINTRAG  *ZEintrag;
#endif

/**********************************************************************/
/*            Bestimmung des Eintrags der Terminaltabelle             */
/**********************************************************************/

{  K_TYP2    TabEintrag;
   unsigned  TIndex;

#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("FT(%d,%d) = \n", Z, S);
#endif /* ZERTEILERTEST */
    for (;;) {

    if (TCheck [TIndex = TBase [Z] + S] == Z) {

      TabEintrag = TNext [TIndex];
#ifdef ZERTEILERTEST
      if (TabellenTest)
        printf("%d == \n", TabEintrag);
#endif /* ZERTEILERTEST */

      if (TabEintrag <= ZEMAXTE1)
      {
        ZEintrag->Aktion = Lies;
        ZEintrag->V.FolgeZustand = TabEintrag;
#ifdef ZERTEILERTEST
        if (TabellenTest)
          printf("Lies und gehe in den Zustand %d\n",
                 ZEintrag->V.FolgeZustand);
#endif /* ZERTEILERTEST */
      }
      else if (TabEintrag <= ZEMAXTE2)
      {
        ZEintrag->Aktion = LiesReduziere;
        ZEintrag->V.ProduktionsNr = TabEintrag - ZEMAXTE1;
#ifdef ZERTEILERTEST
        if (TabellenTest)
          printf("Lies und Reduziere Produktion %d\n",
                 ZEintrag->V.ProduktionsNr);
#endif /* ZERTEILERTEST */
      }
      else
      {
        ZEintrag->Aktion = Reduziere;
        ZEintrag->V.ProduktionsNr = TabEintrag - ZEMAXTE2;
#ifdef ZERTEILERTEST
        if (TabellenTest)
          printf("Reduziere Produktion %d\n",
                 ZEintrag->V.ProduktionsNr);
#endif /* ZERTEILERTEST */
      } /* end if */

      break;

    } else {

      if ((Z = TZDefault [Z]) == KEINZUSTAND) {
        ZEintrag->Aktion = BehandleFehler;
        break;
      }
#ifdef MESSUNG
      DefaultZaehler++;
#endif /* MESSUNG */

    } /* end if */

    } /* end for */


}

#if defined(__cplusplus) || defined(__STDC__) 
static void
NTabelle(K_TYP2 Z, K_TYP2 N, TABELLENEINTRAG  *ZEintrag)
#else
static void
NTabelle(Z, N, ZEintrag)
K_TYP2            Z, N;
TABELLENEINTRAG  *ZEintrag;
#endif

/**********************************************************************/
/*            Bestimmung des Eintrags der Nichtterminaltabelle        */
/**********************************************************************/

{  K_TYP2  TabEintrag;

    TabEintrag = NtNext [NtBase [Z] + N];
#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("FN(%d,%d) = \n", Z, N);

    if (TabellenTest)
      printf("%d == \n", TabEintrag);
#endif /* ZERTEILERTEST */

    if (TabEintrag <= ZEMAXNTE)
    { 
      ZEintrag->Aktion = LiesNT;
      ZEintrag->V.FolgeZustand = TabEintrag;
#ifdef ZERTEILERTEST
      if (TabellenTest)
        printf("Lies NT und gehe in den Zustand %d\n",
               ZEintrag->V.FolgeZustand);
#endif /* ZERTEILERTEST */
    }
    else
    {
      ZEintrag->Aktion = LiesNTReduziere;
      ZEintrag->V.ProduktionsNr = TabEintrag - ZEMAXNTE;
#ifdef ZERTEILERTEST
      if (TabellenTest)
        printf("Lies NT und Reduziere Produktion %d\n",
               ZEintrag->V.ProduktionsNr);
#endif /* ZERTEILERTEST */
    }

}

#if defined(__cplusplus) || defined(__STDC__) 
static  int
InMenge(M_TYP *ZM, int X)
#else
static  int
InMenge(ZM, X)
M_TYP          *ZM;
int  X;
#endif
{
  return ( INMENGE(ZM, X) ); 
}

#ifdef VERBESSERUNG
/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung - ZertVorschau          */
/**********************************************************************/
#if defined(__cplusplus) || defined(__STDC__) 
static void
ZertVorschau(K_TYP2 *ZKel, int Peg, int Anf)
#else
static void
ZertVorschau(ZKel, Peg, Anf) 
K_TYP2    *ZKel;
int   Peg;
int   Anf;
#endif

/* Zerteilt modifizierte Vorschau von Anf an und prueft, ob darin  */
/* ein syntaktischer Fehler auftritt.                              */

{
  TABELLENEINTRAG   E;
  unsigned          i;

  if (!Kel) Kel = NewZerteilerkeller;
  for (i = 0; i <= Peg; i++)
    Kel[i] = *(ZKel + i);

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* ZertVorschau Anf = %4d\n", Anf);
#endif /* ZERTEILERTEST */

  TTabelle(Kel[Peg], T_CODE(Vorschau[Anf]), &E);

  while(1)
  {
#ifdef ZERTEILERTEST
    if (ZerteilerTest)
      DruckeZerteilerZustand(Kel[Peg], T_CODE(Vorschau[Anf]), &E);
#endif /* ZERTEILERTEST */
    if (Peg == StackSize) Expand();
    Peg++;

    switch (E.Aktion)
    {
      case Lies :
      case LiesReduziere :
        if (++Anf > VorschauEnde)
          goto ZerteileEnde;
        break;
      case Reduziere :
        Peg--;
        break;
      case LiesNT :
        if (E.V.FolgeZustand == STARTZUSTAND)
          goto ZerteileEnde;
        break;
      case LiesNTReduziere :
      case BehandleFehler :
        break;
    }

    switch (E.Aktion)
    {
      case BehandleFehler :
        goto ZerteileEnde;
        break;
      case Lies :
      case LiesNT :
        Kel[Peg] = E.V.FolgeZustand;
        TTabelle(Kel[Peg], T_CODE(Vorschau[Anf]), &E);
        break;
      case Reduziere :
      case LiesReduziere :
      case LiesNTReduziere :
        Peg -= LaengeRS[E.V.ProduktionsNr];
        NTabelle(Kel[Peg], LS[E.V.ProduktionsNr], &E);
        break;
    }
  }

  ZerteileEnde :
  verbessert = E.Aktion != BehandleFehler;
}

/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung - LiesVorschau          */
/**********************************************************************/
static void
LiesVorschau()

/* Schiebt den Vorschaupuffer weiter nach rechts und fuellt ihn */
/* ggf. auf                                                     */

{
  unsigned  i;

  if (VorschauAnfang == VorschauEnde)
    Vorschau[VorschauEnde] = GrundSym;
  for (i = VorschauAnfang; i <= VorschauEnde; i++)
    Vorschau[ i - VorschauAnfang + 1 ] = Vorschau[i];
  VorschauEnde = VorschauEnde - VorschauAnfang + 1;
  VorschauAnfang = 1;
  while ( (T_CODE(Vorschau[VorschauEnde]) != ZESTOPSYMBOL) &&
          (VorschauEnde < MAXVORSCHAU - 1) )
    GET_TOKEN(Vorschau[++VorschauEnde]);
}

/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung - Einfuegung            */
/**********************************************************************/
static void
Einfuegung()
{
  int  t;

  t = -1;  /* ZEMINTERMINALCODE */
  VorschauAnfang--;

  do
  { if (++t != ZESTOPSYMBOL)
    { FehlerDeskriptor(t, &T_POS(Vorschau[VorschauAnfang + 1]), 
                       &Vorschau[VorschauAnfang]);
      ZertVorschau(Keller, Pegel, VorschauAnfang);
    }
  } while ( (t != ZEMAXTERMINALCODE) && !verbessert );

  if (verbessert)
  { FehlerMeldung(KOMMENTAR, AUFSETZPUNKT,
                  &T_POS(Vorschau[VorschauAnfang + 1]),
                  (GRUNDSYMBOLDESKRIPTOR *) 0);
    FehlerMeldung(REPARATUR, SYMBOLEINGEFUEGT,
                  &T_POS(Vorschau[VorschauAnfang]),
                  &Vorschau[VorschauAnfang]);
  }
  else
    VorschauAnfang++;
  
#ifdef ZERTEILERTEST
  if ( (ZerteilerTest || TestZerteiler) && verbessert)
    printf("?FB* %3d %5d = Symbol eingefuegt (Verbesserung)\n",
           FUEGTEIN, T_CODE(Vorschau[VorschauAnfang]));
#endif /* ZERTEILERTEST */
} 

/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung - Ersetzung             */
/**********************************************************************/
static void
Ersetzung()
{
  int                    t;
  GRUNDSYMBOLDESKRIPTOR  G;

  t = -1;
  G = Vorschau[VorschauAnfang];

  do
  { if (++t != ZESTOPSYMBOL)
    { FehlerDeskriptor(t, &T_POS(Vorschau[VorschauAnfang]), 
                       &Vorschau[VorschauAnfang]);
      ZertVorschau(Keller, Pegel, VorschauAnfang);
    }
  } while ( (t != ZEMAXTERMINALCODE) && !verbessert );

  if (!verbessert)
    Vorschau[VorschauAnfang] = G;

#ifdef ZERTEILERTEST
  if (ZerteilerTest && verbessert)
    printf("*FB* Ersetzung = %4d\n",
           T_CODE(Vorschau[VorschauAnfang]));
  if (TestZerteiler && verbessert)
    printf("?FB* %3d %5d ?FB* %3d %5d\n", UEBERLIEST, T_CODE(G),
           FUEGTEIN, T_CODE(Vorschau[VorschauAnfang]));
#endif /* ZERTEILERTEST */

  if (verbessert)
  { FehlerMeldung(KOMMENTAR, AUFSETZPUNKT,
                  &T_POS(Vorschau[VorschauAnfang + 1]),
                  (GRUNDSYMBOLDESKRIPTOR *) 0);
    FehlerMeldung(REPARATUR, SYMBOLGELOESCHT, &T_POS(G), &G);
    FehlerMeldung(REPARATUR, SYMBOLEINGEFUEGT,
                  &T_POS(Vorschau[VorschauAnfang]),
                  &Vorschau[VorschauAnfang]);
  }
}

/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung - Loeschung             */
/**********************************************************************/
static void
Loeschung()
{
  ZertVorschau(Keller, Pegel, VorschauAnfang + 1);
  if (verbessert)
  {
    FehlerMeldung(KOMMENTAR, AUFSETZPUNKT,
                  &T_POS(Vorschau[++VorschauAnfang]),
                  (GRUNDSYMBOLDESKRIPTOR *) 0);
    FehlerMeldung(REPARATUR, SYMBOLGELOESCHT,
                  &T_POS(Vorschau[VorschauAnfang-1]),
                  &Vorschau[VorschauAnfang-1]);
  }

#ifdef ZERTEILERTEST
  if ( (ZerteilerTest || TestZerteiler) && verbessert)
    printf("?FB* %3d %5d = Symbol geloescht\n", UEBERLIEST, 
           T_CODE(Vorschau[VorschauAnfang -1]));
#endif /* ZERTEILERTEST */
}

/**********************************************************************/
/*            Fehlerbehandlung - Verbesserung                         */
/**********************************************************************/
static void
Verbesserung()

/* Versucht durch Einfuegung, Ersetzung oder Loeschung eines Symbols  */
/* den Fehler zu beheben.                                             */

{

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Verbesserung\n");
#endif /* ZERTEILERTEST */

  LiesVorschau();
  verbessert = 0;
  Einfuegung();
  if (T_CODE(Vorschau[VorschauAnfang]) != ZESTOPSYMBOL)
  { if (!verbessert)
      Ersetzung();
    if (!verbessert)
      Loeschung();
    if (verbessert)
    { GrundSym = Vorschau[VorschauAnfang];
      TTabelle(Keller[Pegel], T_CODE(GrundSym), &Eintrag);
    }
  }

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    if (verbessert)
      printf("*FB* Verbesserung gelungen\n");
    else
      printf("*FB* Verbesserung misslungen\n");
#endif /* ZERTEILERTEST */

}
#endif /* VERBESSERUNG */


/**********************************************************************/
/*            Fehlerbehandlung - aufgesetzt                           */
/**********************************************************************/
#if defined(__cplusplus) || defined(__STDC__) 
static int
aufgesetzt(K_TYP2 Z)
#else
static int
aufgesetzt(Z)
K_TYP2  Z;
#endif

/* aufgesetzt = 1, falls im Zustand Z auf T_CODE(GrundSym)     */
/*                 aufgesetzt werden kann.                        */
/* aufgesetzt = 0, sonst.                                         */

{
  TABELLENEINTRAG  E;

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Pruefe %5d\n", Z);
#endif /* ZERTEILERTEST */
  TTabelle(Z, T_CODE(GrundSym), &E);
  return ( (E.Aktion == Lies) || (E.Aktion == LiesReduziere) );
}

/**********************************************************************/
/*            Fehlerbehandlung - FehlerSym                            */
/**********************************************************************/
#if defined(__cplusplus) || defined(__STDC__) 
static int
FehlerSym(K_TYP2 *ZK, unsigned P)
#else
static int
FehlerSym(ZK, P)
K_TYP2           *ZK;
unsigned          P;
#endif

/* FehlerSym = RSymbol[K[P]], falls RSymbol[K[P]] Separator ist und */
/* sich der Aufsetzpunkt dahinter befindet.                         */
/* FehlerSym = FSymbol[K[P]], sonst.                                */

{
  int    F, R;
  K_TYP2            Z;
  TABELLENEINTRAG   EF, ER;
  int               Separator;

  Z = *(ZK + P);
  F = FSymbol[Z]; R = RSymbol[Z];
  if (F == R)
    return F;
  else
  {
    TTabelle(Z, F, &EF); TTabelle(Z, R, &ER);
    if (ER.Aktion != EF.Aktion)
      Separator = 1;
    else
    { switch (ER.Aktion)
      { case BehandleFehler :
          Separator = 0;
          break;
        case Lies :
        case LiesNT :
          Separator = ER.V.FolgeZustand != EF.V.FolgeZustand;
          break;
        case Reduziere :
        case LiesReduziere :
        case LiesNTReduziere :
          Separator = ER.V.ProduktionsNr != EF.V.ProduktionsNr;
          break;
      }
    }
    if (Separator)
    {
      /* RSymbol[K[P]] ist Separator. Nun ist zu pruefen, ob sich  */
      /* dahinter der Aufsetzpunkt befindet.                       */
#ifdef ZERTEILERTEST
      if (ZerteilerTest)
        printf("*FB* Schaue hinter Separator\n");
#endif /* ZERTEILERTEST */
      while (1)
      { if (ER.Aktion == Reduziere)
          P--;
        switch (ER.Aktion)
        { case BehandleFehler :
            Abbruch(UEBERSETZERFEHLER, TABELLENFALSCH, &T_POS(GrundSym));
            break;
          case Lies :
          case LiesNT :
            goto ZerteileEnde;
            /* Comment out the break below to avoid compiler warnings about */
            /* an unreachable statement.                                    */
            /* break; */
          case Reduziere :
          case LiesReduziere :
          case LiesNTReduziere :
            P = P - LaengeRS[ER.V.ProduktionsNr] + 1;
            NTabelle(*(ZK + P), LS[ER.V.ProduktionsNr], &ER);
            break;
        } /* end switch */
      } /* end while */

      ZerteileEnde :
#ifdef ZERTEILERTEST
      if (ZerteilerTest)
        printf("*FB* Hinter Separator geschaut\n");
#endif /* ZERTEILERTEST */
      
      if (aufgesetzt(ER.V.FolgeZustand))
        return R;
      else
        return F;

    } /* end if */
    else return F;
  } /* end if */
}

/**********************************************************************/
/*            Fehlerbehandlung - SucheAufsetzPunkt - Erreichbar       */
/**********************************************************************/
#if defined(__cplusplus) || defined(__STDC__) 
static void
Erreichbar(K_TYP2 *ZKel, unsigned Peg, int SemKl)
#else
static void
Erreichbar(ZKel, Peg, SemKl)
K_TYP2    *ZKel;
unsigned   Peg;
int        SemKl;
#endif
{
  int		   F = 0;
  K_TYP2           Z;
  unsigned         i;
  TABELLENEINTRAG  E;
  int              halt;

  if (!Kel) Kel = NewZerteilerkeller;
  for (i = 0; i <= Peg; i++)
    Kel[i] = *(ZKel + i);

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Pruefe Erreichbarkeit fuer %5d\n", 
           T_CODE(GrundSym));
#endif /* ZERTEILERTEST */

  /* Zunaechst wird mittels einer Kopie des Zerteilerzustandes durch  */
  /* Fortsetzung der Zerteilung geprueft, ob das aktuelle Grundsymbol */
  /* akzeptiert werden kann. Durch Erniedrigung des Kellerpegels wird */
  /* dafuer gesorgt, dass auch der aktuelle Zustand geprueft wird.    */

  E.Aktion =  LiesNT; E.V.FolgeZustand = Kel[Peg]; Peg--;
  halt = 0;

  while (1)
  {
    if (Peg == StackSize) Expand();
    Peg++;

    switch(E.Aktion)
    {
      case Lies :
      case LiesReduziere :
        halt = (!SemKl) && InMenge(SemKlammer, F);
        break;
      case Reduziere :
        Peg--;
        break;
      case LiesNT :
      case LiesNTReduziere :
      case BehandleFehler :
        break;
    }

    switch(E.Aktion)
    {
      case BehandleFehler :
        Abbruch(UEBERSETZERFEHLER, TABELLENFALSCH, &T_POS(GrundSym));
        break;

      case Lies :
      case LiesNT :
        Z = E.V.FolgeZustand;
        Kel[Peg] = Z;
        if (aufgesetzt(Z))
        { gefunden = 1; halt = 1;
        }
        else
        { F = FehlerSym(Kel, Peg);
          TTabelle(Z, F, &E);
#ifdef ZERTEILERTEST
          if (ZerteilerTest)
            DruckeZerteilerZustand(Z, F, &E);
#endif /* ZERTEILERTEST */
        }
        break;
    
      case Reduziere :
      case LiesReduziere :
      case LiesNTReduziere :
        Peg -= LaengeRS[E.V.ProduktionsNr];
        NTabelle(Kel[Peg], LS[E.V.ProduktionsNr], &E);
        break;

    } /* end switch */

    if (halt || ((E.Aktion == LiesNT) &&
                 (E.V.FolgeZustand == STARTZUSTAND)))
      goto ZerteileEnde;

  } /* end while */

  ZerteileEnde : ;
#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    if (gefunden)
      printf("*FB* Erreicht\n");
    else
      if (halt)
        printf("*FB* Abbruch wegen SemKlammer\n");
      else
        printf("*FB* Zu Ende gesucht\n");
#endif /* ZERTEILERTEST */
}

/**********************************************************************/
/*            Fehlerbehandlung - SucheAufsetzPunkt                    */
/**********************************************************************/
static void
SucheAufsetzPunkt()

/* Es wird ein akzeptables Symbol gesucht, wobei eventuell Symbole  */
/* ueberlesen werden.                                               */

{
#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* SucheAufsetzPunkt\n");
#endif /* ZERTEILERTEST */

  gefunden = 0;
  do
  { if (T_CODE(GrundSym) == ZESTOPSYMBOL)
      gefunden = 1;
    else
      if (!InMenge(Ueberlesen, T_CODE(GrundSym)))
        Erreichbar(Keller, Pegel,
                   InMenge(SemKlammer, T_CODE(GrundSym)));
    if (!gefunden)
    {
#ifdef ZERTEILERTEST
      if (ZerteilerTest || TestZerteiler)
        printf("?FB* %3d %5d = Symbol ueberlesen\n", UEBERLIEST, 
               T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
      FehlerMeldung(REPARATUR, SYMBOLGELOESCHT, &T_POS(GrundSym),
                    &GrundSym);
#ifdef VERBESSERUNG
      if (VorschauAnfang < VorschauEnde)
        GrundSym = Vorschau[++VorschauAnfang];
      else
#endif /* VERBESSERUNG */
      GET_TOKEN(GrundSym);
    }
  } while (!gefunden);

  FehlerMeldung(KOMMENTAR, AUFSETZPUNKT, &T_POS(GrundSym),
                (GRUNDSYMBOLDESKRIPTOR *) 0);
}


/**********************************************************************/
/*            Fehlerbehandlung - ErreicheAufsetzPunkt                 */
/**********************************************************************/
static void
ErreicheAufsetzPunkt()
{
  int           F;
  K_TYP2                   Z;
  GRUNDSYMBOLDESKRIPTOR    FDeskr;
  int                      halt;

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Erreiche Aufsetzpunkt\n");
#endif /* ZERTEILERTEST */

  /* Nun werden die gleichen Zerteileraktionen wie in Erreichbar  */
  /* wiederholt, diesmal aber mit semantischen Aktionen. Am Ende  */
  /* gilt die Invariante der Hauptzerteilerschleife wieder.       */
  /* Zunaechst wird wie in Erreichbar der Pegel zurueckgesetzt.   */

  if (aufgesetzt(Keller[Pegel])) {
    TTabelle(Keller[Pegel], T_CODE(GrundSym), &Eintrag);
    return;
  }
  halt = 0;
  F = FehlerSym(Keller, Pegel);
  TTabelle(Keller[Pegel], F, &Eintrag);

  while (1)
  {
    switch(Eintrag.Aktion)
    {
      case Lies :
      case LiesReduziere :
        FehlerDeskriptor(F, &T_POS(GrundSym), &FDeskr);
        FehlerMeldung(REPARATUR, SYMBOLEINGEFUEGT, &T_POS(FDeskr),
                      &FDeskr);
#ifdef ZERTEILERTEST
        if (ZerteilerTest || TestZerteiler)
          printf("?FB* %3d %5d = Symbol eingefuegt\n", FUEGTEIN, F);
#endif /* ZERTEILERTEST */
        AttributKeller[Pegel] = FDeskr;
#ifdef ZERTEILERTEST
        if (TestZerteiler)
          printf("?FB* %3d %5d\n", KELLERSYMBOL, F);
#endif /* ZERTEILERTEST */

      case LiesNT :
      case LiesNTReduziere :
        if(Pegel == StackSize) Expand();
        Pegel++;
        AttributKeller[Pegel] = FDeskr;
        break;

      case Reduziere :
      case BehandleFehler :
        break;
    }

    switch(Eintrag.Aktion)
    {
      case BehandleFehler :
        Abbruch(UEBERSETZERFEHLER, TABELLENFALSCH, &T_POS(GrundSym));
        break;
      
      case Lies :
      case LiesNT :
        Z = Eintrag.V.FolgeZustand;
        Keller[Pegel] = Z;
        if (aufgesetzt(Z))
        { halt = 1;
          TTabelle(Z, T_CODE(GrundSym), &Eintrag);
        }
        else
        { F = FehlerSym(Keller, Pegel);
          TTabelle(Z, F, &Eintrag);
        }
        break;
                  
      case Reduziere :
      case LiesReduziere :
      case LiesNTReduziere :
#ifdef ZERTEILERTEST
        if (ZerteilerTest || TestZerteiler)
          printf("?FB* %3d %5d = Produktion reduziert\n", 
                 REDUZIERT, Eintrag.V.ProduktionsNr);
#endif /* ZERTEILERTEST */
#ifdef RIGHTCOORD
	{
	    int LngRS = LaengeRS[Eintrag.V.ProduktionsNr];
            if (LngRS == 0) {
                POSITION *p = &T_POS (*(AttributKeller + Pegel));
                LineOf (rightpos) = RLineOf (rightpos) = LineOf (*p);
                ColOf (rightpos) = RColOf (rightpos) = ColOf (*p);
#ifdef MONITOR
                CumColOf (rightpos) = RCumColOf (rightpos) = CumColOf (*p);
#endif
	    } else {
                POSITION *p = &T_POS (*(AttributKeller + Pegel - 1));
		rightpos = T_POS (*(AttributKeller + Pegel - LngRS));
                RLineOf (rightpos) = RLineOf (*p);
                RColOf (rightpos) = RColOf (*p);
#ifdef MONITOR
	        RCumColOf (rightpos) = RCumColOf (*p);
#endif
	    }
	}
#endif
        Pegel -= LaengeRS[Eintrag.V.ProduktionsNr];
        StrukturAnknuepfung(Eintrag.V.ProduktionsNr, AttributKeller + Pegel);
#ifdef RIGHTCOORD
#ifdef MONITOR
        _dapto_production (Eintrag.V.ProduktionsNr,
                           ntlen[conc_prodmap[Eintrag.V.ProduktionsNr]],
                           LineOf (rightpos), CumColOf (rightpos),
                           RLineOf (rightpos), RCumColOf (rightpos));
#endif
        T_POS (*(AttributKeller + Pegel)) = rightpos;
#endif
        NTabelle(Keller[Pegel], LS[Eintrag.V.ProduktionsNr], &Eintrag);
        break;
    } /* end switch */

    if (halt || ((Eintrag.Aktion == LiesNT) &&
                 (Eintrag.V.FolgeZustand == STARTZUSTAND)))
      goto ZerteileEnde;

  } /* end while */

  ZerteileEnde:
  ;  
#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Aufsetzpunkt erreicht\n");
#endif /* ZERTEILERTEST */
}

/**********************************************************************/
/*            Instandsetzung bei syntaktischen Fehlern                */
/**********************************************************************/

static void
FehlerBehandlung ()
{

#ifdef ZERTEILERTEST
  if (ZerteilerTest)
    printf("*FB* Fehlerbehandlung\n");
  if (TestZerteiler)
    printf("?FB* %3d %5d\n?FB* %3d %5d\n", FEHLERZUSTAND, 
           Eintrag.V.FolgeZustand, FEHLERSYMBOL, T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
  
  FehlerMeldung(SCHWERERFEHLER, SYNTAXFEHLER, &T_POS(GrundSym),
                (GRUNDSYMBOLDESKRIPTOR *) 0);
  
#ifdef VERBESSERUNG
#ifdef ZERTEILERTEST
  if (KeineVerbesserung)
    verbessert = 0;
  else
#endif /* ZERTEILERTEST */
  Verbesserung();
  if (!verbessert)
  {
#endif /* VERBESSERUNG */
  SucheAufsetzPunkt();
  ErreicheAufsetzPunkt();
#ifdef VERBESSERUNG
  }
#endif /* VERBESSERUNG */
  
#ifdef ZERTEILERTEST
  if (ZerteilerTest) 
    printf("*FB* Fehler behandelt\n");
#endif /* ZERTEILERTEST */
}


/**********************************************************************/
/*            Anfang des Zerteilers                                   */
/**********************************************************************/

void  Zerteiler()
{
  register  K_TYP2  *ZKeller;
  register  GRUNDSYMBOLDESKRIPTOR  *ZAttributKeller;
  register  unsigned  xZP;
  /* xZP enthaelt den Folgezustand bei einer Lies oder LiesNT   */
  /* Aktion. Erfolgt eine LiesReduziere, LiesNTReduziere oder   */
  /* Reduziere Aktion, so enthaelt xZP die Nummer der zu redu-  */
  /* zierenden Produktion. Daneben wird xZP beim Zugriff zur    */
  /* Terminal- bzw. Nichtterminaltabelle als Hilfsvariable zur  */
  /* Bestimmung der naechsten Zerteileraktion benutzt.          */

#ifdef MONITOR
  _dapto_enter ("parser");
#endif

  if (!Keller)
    Keller = NewZerteilerkeller;

  ZKeller = Keller;

  if (!AttributKeller)
    AttributKeller = NewAttributkeller;

  ZAttributKeller = AttributKeller;

  xZP = STARTZUSTAND;
#ifdef MESSUNG
  StartZeit = CPUTime();
#endif /* MESSUNG */

xLies :
#ifdef ZERTEILERTEST
  if (TestZerteiler)
    printf("?ZT* %3d%5d\n", LIEST, T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
#ifdef ZERTEILERTEST
  if (TestZerteiler)
    printf("?ZT* %3d%5d\n", KELLERSYMBOL, T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
#ifdef VERBESSERUNG
  if (VorschauAnfang < VorschauEnde)
    GrundSym = Vorschau[++VorschauAnfang];
  else
#endif /* VERBESSERUNG */
  GET_TOKEN(GrundSym);
#ifdef MESSUNG
  LeseZaehler++;
#endif /* MESSUNG */
xLiesNT :
  *++ZAttributKeller = GrundSym;
  *++ZKeller = xZP;
  if (ZKeller == Keller + StackSize) {
    Pegel = ZKeller - Keller;
    Expand();
    ZKeller = Keller + Pegel;
    ZAttributKeller = AttributKeller + Pegel;
  }
  /* Der Keller muss nur bei einer Lies oder LiesNT Aktion auf  */
  /* Ueberlauf geprueft werden, sofern fuer eine nachfolgende   */
  /* LiesReduziere oder LiesNTReduziere Aktion noch eine Keller-*/
  /* zelle frei bleibt.                                         */

/****************************************************************/
/*            Zugriff auf die Terminaltabelle                   */
/****************************************************************/
  {
    register int  S;
    unsigned  TIndex;

    S = T_CODE(GrundSym);

#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("FT(%d,%d) = \n", xZP, S);
#endif /* ZERTEILERTEST */
    for (;;)
    {

      if (TCheck [TIndex = TBase [xZP] + S] == xZP)
      {

        xZP = TNext [TIndex];
#ifdef ZERTEILERTEST
        if (TabellenTest)
          printf("%d == \n", xZP);
#endif /* ZERTEILERTEST */

        if (xZP <= ZEMAXTE1)
        {
#ifdef MESSUNG
          AktionsZaehler[(int)Lies]++;
#endif /* MESSUNG */
#ifdef ZERTEILERTEST
          if (TabellenTest)
            printf("Lies und gehe in den Zustand %d\n", xZP);
#endif /* ZERTEILERTEST */
          goto xLies;
        }
        else if (xZP <= ZEMAXTE2)
        {
#ifdef MESSUNG
          AktionsZaehler[(int)LiesReduziere]++;
#endif /* MESSUNG */
          xZP -= ZEMAXTE1;
#ifdef ZERTEILERTEST
          if (TabellenTest)
            printf("Lies und Reduziere Produktion %d\n", xZP);
#endif /* ZERTEILERTEST */
          goto xLiesReduziere;
        }
        else
        {
#ifdef MESSUNG
          AktionsZaehler[(int)Reduziere]++;
#endif /* MESSUNG */
          xZP -= ZEMAXTE2;
#ifdef ZERTEILERTEST
          if (TabellenTest)
            printf("Reduziere Produktion %d\n", xZP);
#endif /* ZERTEILERTEST */
          goto xReduziere;
        }

      }
      else
      {
        if ((xZP = TZDefault [xZP]) == KEINZUSTAND) {
          if (!Reparatur(
                 &(T_POS(GrundSym)),
                 &(T_CODE(GrundSym)),
                 &(T_ATTR(GrundSym)))) goto xBehandleFehler;
          *ZAttributKeller = GrundSym;
          S = T_CODE(GrundSym) ;
          xZP = *ZKeller;
        }
#ifdef MESSUNG
        DefaultZaehler++;
#endif /* MESSUNG */
      }

    } /* end for */

  }
/**********************************************************************/

xLiesReduziere:
#ifdef ZERTEILERTEST
  if (TestZerteiler)
    printf("?ZT* %3d%5d\n", LIEST, T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
#ifdef ZERTEILERTEST
  if (TestZerteiler)
    printf("?ZT* %3d%5d\n", KELLERSYMBOL, T_CODE(GrundSym));
#endif /* ZERTEILERTEST */
#ifdef VERBESSERUNG
  if (VorschauAnfang < VorschauEnde)
    GrundSym = Vorschau[++VorschauAnfang];
  else
#endif /* VERBESSERUNG */
  GET_TOKEN(GrundSym);
#ifdef MESSUNG
  LeseZaehler++;
#endif /* MESSUNG */
xLiesNTReduziere:
  *++ZAttributKeller = GrundSym;
  ZKeller++;
xReduziere:
#ifdef ZERTEILERTEST
  if (ZerteilerTest || TestZerteiler)
    printf("?ZT* %3d%5d d=%d\n", REDUZIERT, xZP, ZKeller - Keller);
#endif /* ZERTEILERTEST */
  {
    register unsigned LngRS;

    LngRS = LaengeRS[xZP];
#ifdef RIGHTCOORD
    if (LngRS == 0) {
      POSITION *p = &T_POS (*(ZAttributKeller));
      LineOf (rightpos) = RLineOf (rightpos) = LineOf (*p);
      ColOf (rightpos) = RColOf (rightpos) = ColOf (*p);
#ifdef MONITOR
      CumColOf (rightpos) = RCumColOf (rightpos) = CumColOf (*p);
#endif
    } else {
      POSITION *p = &T_POS (*(ZAttributKeller - 1));
      rightpos = T_POS (*(ZAttributKeller - LngRS));
      RLineOf (rightpos) = RLineOf (*p);
      RColOf (rightpos) = RColOf (*p);
#ifdef MONITOR
      RCumColOf (rightpos) = RCumColOf (*p);
#endif
    }
#endif
    ZKeller -= LngRS;
    ZAttributKeller -= LngRS;
  }
  StrukturAnknuepfung(xZP, ZAttributKeller);

#ifdef RIGHTCOORD
#ifdef MONITOR
  _dapto_production (xZP, ntlen[conc_prodmap[xZP]],
                     LineOf (rightpos), CumColOf (rightpos),
                     RLineOf (rightpos), RCumColOf (rightpos));
#endif
  T_POS (*ZAttributKeller) = rightpos;
#endif

/**********************************************************************/
/*            Zugriff auf die Nichtterminaltabelle                    */
/**********************************************************************/

#ifdef ZERTEILERTEST
  if (TabellenTest)
    printf("FN(%d,%d) = \n", *ZKeller, LS [xZP]);
#endif /* ZERTEILERTEST */
  xZP = NtNext [NtBase [*ZKeller] + LS [xZP]];
#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("%d == \n", xZP);
#endif /* ZERTEILERTEST */

  if (xZP <= ZEMAXNTE)
  { 
#ifdef MESSUNG
    AktionsZaehler[(int)LiesNT]++;
#endif /* MESSUNG */
#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("Lies NT und gehe in den Zustand %d\n", xZP);
#endif /* ZERTEILERTEST */
    if (xZP == STARTZUSTAND)
      goto ZerteilerEnde;
    else
      goto xLiesNT;
  }
  else
  {
#ifdef MESSUNG
    AktionsZaehler[(int)LiesNTReduziere]++;
#endif /* MESSUNG */
    xZP -= ZEMAXNTE;
#ifdef ZERTEILERTEST
    if (TabellenTest)
      printf("Lies NT und Reduziere Produktion %d\n", xZP);
#endif /* ZERTEILERTEST */
    goto xLiesNTReduziere;
  }

/**********************************************************************/

xBehandleFehler :
#ifdef MESSUNG
  AktionsZaehler[(int)BehandleFehler]++;
#endif /* MESSUNG */
  Eintrag.Aktion = BehandleFehler;

  Pegel = ZKeller - Keller;

  FehlerBehandlung();

  if (Pegel == StackSize) Expand();

  ZKeller = Keller + Pegel;
  ZAttributKeller = AttributKeller + Pegel;
  *ZAttributKeller = GrundSym;

#ifdef MESSUNG
  AktionsZaehler[(int)Eintrag.Aktion]++;
#endif /* MESSUNG */
  if (Eintrag.Aktion == LiesNT &&
      Eintrag.V.FolgeZustand == STARTZUSTAND) goto ZerteilerEnde;

  switch (Eintrag.Aktion)
  {
    case Lies :
      xZP = Eintrag.V.FolgeZustand;
      goto xLies; 

    case LiesReduziere :
      xZP = Eintrag.V.ProduktionsNr;
      goto xLiesReduziere;

#ifdef VERBESSERUNG
    case Reduziere :
      xZP = Eintrag.V.ProduktionsNr;
      goto xReduziere;

#endif /* VERBESSERUNG */
    default :
      Abbruch(UEBERSETZERFEHLER, TABELLENFALSCH, &T_POS(GrundSym));
      break;
   }

ZerteilerEnde: ;

#ifdef MESSUNG

  StopZeit = CPUTime();
  ZerteilungsZeit = StopZeit - StartZeit;
  printf("*ZT* Zerteilungszeit           %10ld Millisekunden\n",
         ZerteilungsZeit);
  printf("*ZT* Aktionen                       Anzahl\n");
  printf("*ZT* BerechneDefaultZustand    %10ld\n",
         DefaultZaehler);
  printf("*ZT* GET_TOKEN           %10ld\n",
         LeseZaehler);
  printf("*ZT* Lies                      %10ld\n",
         AktionsZaehler[(int)Lies]);
  printf("*ZT* Reduziere                 %10ld\n",
         AktionsZaehler[(int)Reduziere]);
  printf("*ZT* LiesReduziere             %10ld\n",
         AktionsZaehler[(int)LiesReduziere]);
  printf("*ZT* LiesNT                    %10ld\n",
         AktionsZaehler[(int)LiesNT]);
  printf("*ZT* LiesNTReduziere           %10ld\n",
         AktionsZaehler[(int)LiesNTReduziere]);
  printf("*ZT* BehandleFehler            %10ld\n",
         AktionsZaehler[(int)BehandleFehler]);
  printf("*ZT* Aktionen gesamt           %10ld\n",
         AktionsZaehler[(int)Lies] +
         AktionsZaehler[(int)Reduziere] +
         AktionsZaehler[(int)LiesReduziere] +
         AktionsZaehler[(int)LiesNT] +
         AktionsZaehler[(int)LiesNTReduziere]);
#endif /* MESSUNG */

#ifdef MONITOR 
  _dapto_leave ("parser");
#endif

}  /* Zerteilerende */
