// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class CFloatTest {

  /**
   * Following are test cases as provided by a x86_64 amd architecture with a 64-bit linux (ubuntu),
   * compiled using gcc in c11 compliance.
   */
  @Test
  public void infTest() {
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat d_n0 = new CFloatNative("-0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);

    CFloat f_1 =
        new CFloatNative(CFloatNativeAPI.ONE_SINGLE.copyWrapper(), CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat d_1 =
        new CFloatNative(CFloatNativeAPI.ONE_DOUBLE.copyWrapper(), CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat ld_1 =
        new CFloatNative(
            CFloatNativeAPI.ONE_LONG_DOUBLE.copyWrapper(), CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(f_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(f_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(d_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(d_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(ld_1.divideBy(cf_f).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_d).toString()).isEqualTo("inf");
    assertThat(ld_1.divideBy(cf_ld).toString()).isEqualTo("inf");

    assertThat(f_n1.divideBy(cf_f).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_d).toString()).isEqualTo("-inf");
    assertThat(f_n1.divideBy(cf_ld).toString()).isEqualTo("-inf");

    assertThat(f_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(d_1.divideBy(d_n0).toString()).isEqualTo("-inf");
    assertThat(ld_1.divideBy(d_n0).toString()).isEqualTo("-inf");

    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);
    assertThat(inf_f.add(inf_f).toString()).isEqualTo("inf");
    assertThat(inf_nf.add(inf_nf).toString()).isEqualTo("-inf");
    assertThat(inf_f.subtract(inf_nf).toString()).isEqualTo("inf");
    assertThat(inf_nf.subtract(inf_f).toString()).isEqualTo("-inf");
  }

  @Test
  public void nanTest() {
    CFloat cf_f = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(cf_f.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_f.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_d.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_d.divideBy(cf_ld).toString()).isEqualTo("-nan");

    assertThat(cf_ld.divideBy(cf_f).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_d).toString()).isEqualTo("-nan");
    assertThat(cf_ld.divideBy(cf_ld).toString()).isEqualTo("-nan");

    CFloat f_1 =
        new CFloatNative(CFloatNativeAPI.ONE_SINGLE.copyWrapper(), CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat f_n1 = new CFloatNative("-1", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat inf_f = f_1.divideBy(cf_f);
    CFloat inf_nf = f_n1.divideBy(cf_f);

    assertThat(inf_f.subtract(inf_f).toString()).isEqualTo("-nan");
    assertThat(inf_nf.subtract(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_f.add(inf_nf).toString()).isEqualTo("-nan");
    assertThat(inf_nf.add(inf_f).toString()).isEqualTo("-nan");

    cf_f = cf_f.divideBy(cf_f);
    assertThat(cf_f.add(f_1).toString()).isEqualTo("-nan");
  }

  @Test
  public void formatTest() {
    CFloat cf_f = new CFloatNative("71236.262625", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat cf_d =
        new CFloatNative("7891274812.82489681243896484375", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat cf_ld =
        new CFloatNative("82173928379128.897125244140625", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat cf_f2 = new CFloatNative("10.0", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(cf_f.toString()).isEqualTo("71236.265625");
    assertThat(cf_f2.toString()).isEqualTo("10.0");
    assertThat(cf_d.toString()).isEqualTo("7891274812.82489681243896484375");
    assertThat(cf_ld.toString()).isEqualTo("82173928379128.897125244140625");

    CFloatNative two = new CFloatNative("2.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    cf_ld = cf_ld.multiply(two);

    assertThat(cf_ld.toString()).isEqualTo("164347856758257.79425048828125");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("168292205320455981.3125");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("172331218248146924864.0");

    for (int i = 0; i < 10; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString()).isEqualTo("176467167486102451060736.0");

    for (int i = 0; i < 800; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString())
        .isEqualTo(
            "1176685619726757694650973653036889804515652541394935327945104207967319975396405043723449926929245309944011749663826014608357167360348998884812228642560158430137541411941079758241360655682022168784213489289673637327812997677674819356237568534523669580117177832308736.0");

    for (int i = 0; i < 15506; i++) {
      cf_ld = cf_ld.multiply(two);
    }
    assertThat(cf_ld.toString())
        .isEqualTo(
            "694661471029285501212945771332116283189597310134176279871043189542821592361034290078606668822051846516619797487990244598469777314072213775651740997048986477928169371715229655255359206162402789701556285027901731276951276932966299893756710604647716529685676844817520947850222003470298462271502609956078769849589900408767485724485967733599194870111268572665770915385732745533077935584814887246079891275768358592541249141208098888465601496657768977980719104688305805446337127467507655600805010440069686957667093439005930743710899465835693162496930922042219965565755872345088979745951059544410264590885533059802634911610112359072280643717850034447378889483354616356534072050883426144896485391839271548200637934784358423828361948121933169485446537718082323096878768326042779833555104832040698943211166866424754054058518687473286474067948292828214615505961268255909034200141046284021014576712140193597581626713649928012759984522574528083050285415720613046688185859795637548551514445633267671655826926175614480816809095786215643662930033739743457417532921030288466815508794482486385556026749829923189097288118396937096410891290928194250769813296387818031308061421013699955828562315519439982067347353925339756023987581503234833828283545902961339322113952589831569641527330790502240134789694938008747222640520135657293347309106779646491458431122937330634928614238016681904060576387983823642493996674208885147238684518016402962625484639057682915438581028172712163704500077695878867640029346320782445488797900101702716568106462610025654794142243666380061530725147800898964271744942918698143898638677164669373415496340898955825715641315678595790331151935905439190907272988737897140776341551472533780997375933937280166567585546743417096516847812195640709165790520762176180645358796498294497937404592297711082877765435291230286449583290834014773608422738804265973087281653073568570051219143973910598323202339617831684547261405588501034825543136062625740678347521636328626362325934600058707526730757605612347610346212727713445867537697290954889015840832006481552544354178598796024443289767222437288540384480913881790894681495830618486454777010193932844297424215592577430441244105631112284969397475586241350252719801955697719248263291576677400674564535012124466537817216674499601502843427706380322070207536266111849178732613493037133143914501656258394138427949341147498750043592117864861596291386955437525765875792978815200711703086031773265094062090433776474312925188037192978252390118103889764695250098400244382524632823218243298009716454263131170334953697375330234224834781529277158940732501033393627628292312518166039974135205583324016714039552252629662543435069986204212642024264748258088696773792510522901517507147404584504975919932787426188933702059345208175186414713367396753093710889099933396533310908624159283761910931570929031203239686518771465176191244443044838834439123817697621124371319307459407210656775350861659923351214137817620919405807574682209639907041498911534486791680147248174965353762937170834652169113699719970650209429514481731839607352056178750857322027687164428090119048609946821756584153887527951824142714772071015481449220282242859175402042471364900016874449405119766458202737518073575124390460229466164486486763731134790535338425159647362988989102415661368726931304389708098839253559241268159331162251596344133526786246671926477664370370204892975840991346964500139158435599256060845711019902324906317364112623636594144730900393698675674366435769737226028433017218491798546804160179402915827547646124889736393626326956158058847406745332212306275800009936633898125529945356843416313313501275104561978090672919846164480810915703489417353646940510996425915489531252616528416221473099427215939525288115469518510884813536389509309975809114935485012665444248884560381299604705974350152817929550833988410426558267784658496954992911562603970289791474655990552071036799681461338387116372476956571874717859729511104422791089338367448599166202680369739168769388326694208316406853213013832919099103563404858174925582486163566094997172082576169828886731599018830716649789663649445507892365369040297910847183802163649290889687899776076477303597955828244302748529695657718469885311506327204991597781008685208927343341458933797554504751392613129528016892940210384002022271833758521477022845254905080750452568603937516959398361005202759397145084274883419089761733733647216599798582928442438858339726851504835078826199077797972437788669165168797349587348145797789965900525131835010904232570351280328538495302926825096721212439857569699203144367206323634988578019367872848819173425685208948688233960869907377512223872994463655157441362032426157102214890200914510170508584825733648066487606047134622221180356046988009722071281017552038723432403711375708998205704619531394674018103728990824986210336710292853665742993819868990892512234593540492795363955983517552859998485133932437974378822687631687953586593586671266279562696266730495279104.0");

    cf_ld = cf_ld.multiply(two);
    assertThat(cf_ld.toString()).isEqualTo("inf");
  }

  @Test
  public void zeroTest() {
    CFloatNative zero = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloatNative nZero = new CFloatNative("-0.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloatNative nOne = new CFloatNative("-1.0", CFloatNativeAPI.FP_TYPE_DOUBLE);

    assertThat(zero.add(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.add(zero).toString()).isEqualTo("0.0");
    assertThat(nZero.add(nZero).toString()).isEqualTo("-0.0");
    assertThat(nZero.subtract(zero).toString()).isEqualTo("-0.0");
    assertThat(zero.subtract(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.subtract(nZero).toString()).isEqualTo("0.0");
    assertThat(zero.subtract(zero).toString()).isEqualTo("0.0");
    assertThat(nOne.multiply(zero).toString()).isEqualTo("-0.0");
    assertThat(nOne.multiply(nZero).toString()).isEqualTo("0.0");
    assertThat(nZero.divideBy(nOne).toString()).isEqualTo("0.0");
    assertThat(zero.divideBy(nOne).toString()).isEqualTo("-0.0");
    assertThat(zero.multiply(nZero).toString()).isEqualTo("-0.0");
  }

  @Test
  public void additionTest() {
    CFloat ten = new CFloatImpl("10", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat five = new CFloatImpl("5", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat nOne = new CFloatImpl("-1", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    CFloatNative p = new CFloatNative(ten.copyWrapper(), ten.getType());
    CFloatNative q = new CFloatNative(five.copyWrapper(), five.getType());
    assertThat(p.toString()).isEqualTo("10.0");
    assertThat(q.toString()).isEqualTo("5.0");

    CFloat res = ten.add(ten);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_DOUBLE);
    assertThat(p.toString()).isEqualTo("20.0");

    res = res.add(ten, ten, five);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_DOUBLE);
    assertThat(p.toString()).isEqualTo("45.0");

    res = res.add(nOne, nOne, nOne);
    p = new CFloatNative(res.copyWrapper(), CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    assertThat(p.toString()).isEqualTo("42.0");
  }

  @Test
  public void additionTest_With_Overflowing_Floats() {
    CFloat a = new CFloatNative("1.00000011920928955078125", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("0.000000059604644775390625", CFloatNativeAPI.FP_TYPE_SINGLE);

    CFloat aI = new CFloatImpl(a.copyWrapper(), a.getType());
    CFloat bI = new CFloatImpl(b.copyWrapper(), b.getType());

    CFloat resI = aI.add(bI);
    CFloat res = new CFloatNative(resI.copyWrapper(), resI.getType());

    assertThat(res.toString()).isEqualTo("1.0000002384185791015625");

    CFloatWrapper wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 23);
    CFloat bIFractioned = new CFloatImpl(wrapper, bI.getType());
    CFloat bFractioned = new CFloatNative(wrapper, b.getType());

    CFloat resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.00000035762786865234375");

    wrapper.setExponent(wrapper.getExponent() - 1);
    resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo(a.add(b, b.add(bFractioned)).toString());
    assertThat(res.toString()).isEqualTo("1.0000002384185791015625");

    wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() ^ bI.getSignBitMask());
    bI = new CFloatImpl(wrapper, bI.getType());
    wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 1);
    CFloat bI2 = new CFloatImpl(wrapper, bI.getType());

    bI = bI.add(bI2);

    resI = aI.add(bI);
    resI2 = aI.add(bI2);

    assertThat(new CFloatNative(resI.copyWrapper(), resI.getType()).toString()).isEqualTo("1.0");
    assertThat(new CFloatNative(resI2.copyWrapper(), resI2.getType()).toString())
        .isEqualTo("1.00000011920928955078125");
  }

  @Test
  public void additionTest_With_Overflowing_Doubles() {
    CFloat a =
        new CFloatNative(
            "1.0000000000000002220446049250313080847263336181640625",
            CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b =
        new CFloatNative(
            "0.00000000000000011102230246251565404236316680908203125",
            CFloatNativeAPI.FP_TYPE_DOUBLE);

    CFloat aI = new CFloatImpl(a.copyWrapper(), a.getType());
    CFloat bI = new CFloatImpl(b.copyWrapper(), b.getType());

    CFloat resI = aI.add(bI);
    CFloat res = new CFloatNative(resI.copyWrapper(), resI.getType());

    assertThat(res.toString()).isEqualTo("1.000000000000000444089209850062616169452667236328125");

    CFloatWrapper wrapper = bI.copyWrapper();
    wrapper.setExponent(wrapper.getExponent() - 52);
    CFloat bIFractioned = new CFloatImpl(wrapper, bI.getType());

    CFloat resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.0000000000000006661338147750939242541790008544921875");

    wrapper.setExponent(wrapper.getExponent() - 1);
    resI2 = resI.add(bI.add(bIFractioned));
    res = new CFloatNative(resI2.copyWrapper(), resI2.getType());

    assertThat(res.toString()).isEqualTo("1.000000000000000444089209850062616169452667236328125");
  }

  @Test
  public void multiplicationTest() {
    CFloat a = new CFloatImpl("2", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatImpl("3", CFloatNativeAPI.FP_TYPE_SINGLE);

    CFloat res = a.multiply(b);
    CFloat cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("6.0");

    res = b.multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("9.0");

    res = b.multiply(b).multiply(a).multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("54.0");

    res = b.multiply(b).multiply(a).multiply(b).multiply(b);
    cRes = new CFloatNative(res.copyWrapper(), res.getType());
    assertThat(cRes.toString()).isEqualTo("162.0");
  }

  @Test
  public void createTest() {
    CFloat a = new CFloatImpl("12345.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat test = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(test.toString()).isEqualTo("12345.0");

    CFloat b = new CFloatImpl("-2345.0", CFloatNativeAPI.FP_TYPE_DOUBLE);
    test = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(test.toString()).isEqualTo("-2345.0");

    a = new CFloatImpl("1235124562371616235", CFloatNativeAPI.FP_TYPE_SINGLE);
    b = new CFloatNative("1235124562371616235", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(b.toString()).isEqualTo("1235124567312171008.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo("1235124567312171008.0");

    a = new CFloatImpl("0.1235124562371616235", CFloatNativeAPI.FP_TYPE_SINGLE);
    b = new CFloatNative("0.1235124562371616235", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(b.toString()).isEqualTo("0.123512454330921173095703125");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo("0.123512454330921173095703125");

    a = new CFloatImpl("8388609", CFloatNativeAPI.FP_TYPE_SINGLE);
    b = new CFloatNative("8388609", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("16777217", CFloatNativeAPI.FP_TYPE_SINGLE);
    b = new CFloatNative("16777217", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("36893488147419103233", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    b = new CFloatNative("36893488147419103233", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("36893488147419103232.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("18446744073709551617", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    b = new CFloatNative("18446744073709551617", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("18446744073709551616.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("36893488147419103235", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    b = new CFloatNative("36893488147419103235", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(b.toString()).isEqualTo("36893488147419103236.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void nativeAdditionTest() {
    CFloat a =
        new CFloatNative(
            new CFloatWrapper(
                17000L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat b =
        new CFloatNative(
            new CFloatWrapper(
                17063L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.add(b).toString())
        .isEqualTo(
            "7524684765169677984239668825841657450662424665126641865225532990851629461374682772298163028925468467204048782456979919923963285225557620642055414520570450567718506657241077846861357026497412026813181329408.0");

    b =
        new CFloatNative(
            new CFloatWrapper(
                17064L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.add(b).toString())
        .isEqualTo(
            "15049369530339355967391567042394575883208572683233827981680059276598297544036903949561899567255911541278292760274770262798041223663282407204681034557628156709126316397968184796941747610323938741049383452672.0");

    b =
        new CFloatNative(
            new CFloatWrapper(
                17065L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.add(b).toString())
        .isEqualTo(
            "30098739060678711932607592866211673730184592072428744465818105142986672330648884709054946153321772296296975911271161371496311753750899146250502480148230824565631238962908427800321562335306106856944808493056.0");

    a =
        new CFloatImpl(
            new CFloatWrapper(
                17000L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    b =
        new CFloatImpl(
            new CFloatWrapper(
                17063L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo(
            "7524684765169677984239668825841657450662424665126641865225532990851629461374682772298163028925468467204048782456979919923963285225557620642055414520570450567718506657241077846861357026497412026813181329408.0");

    b =
        new CFloatImpl(
            new CFloatWrapper(
                17064L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo(
            "15049369530339355967391567042394575883208572683233827981680059276598297544036903949561899567255911541278292760274770262798041223663282407204681034557628156709126316397968184796941747610323938741049383452672.0");

    b =
        new CFloatImpl(
            new CFloatWrapper(
                17065L, 0b11000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L),
            CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(new CFloatNative(a.add(b).copyWrapper(), a.getType()).toString())
        .isEqualTo(
            "30098739060678711932607592866211673730184592072428744465818105142986672330648884709054946153321772296296975911271161371496311753750899146250502480148230824565631238962908427800321562335306106856944808493056.0");
  }

  @Test
  public void maskTest() {
    assertThat(-1)
        .isEqualTo(0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L);
  }

  @Test
  public void subtractionOverflowTest() {
    CFloatWrapper wrapperA =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111111L,
            0b00000000_00000000_00000000_00000000_00000000_00100000_00000000_00000001L);

    CFloatWrapper wrapperB =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111101L,
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L);

    CFloat aI = new CFloatImpl(wrapperA, CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat bI = new CFloatImpl(wrapperB, aI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(0);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(0);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(383);

    wrapperB.setMantissa(
        0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(1);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(1);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(383);

    wrapperA =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111111L,
            0b10100000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);

    wrapperB =
        new CFloatWrapper(
            0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_01111101L,
            0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L);

    aI = new CFloatImpl(wrapperA, CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    bI = new CFloatImpl(wrapperB, aI.getType());

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775808L);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775808L);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(32895);

    wrapperB.setMantissa(
        0b10000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L);

    assertThat(aI.subtract(bI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775807L);
    assertThat(bI.subtract(aI).copyWrapper().getMantissa()).isEqualTo(-9223372036854775807L);

    assertThat(aI.subtract(bI).copyWrapper().getExponent()).isEqualTo(127);
    assertThat(bI.subtract(aI).copyWrapper().getExponent()).isEqualTo(32895);

    CFloat a = new CFloatNative("12345.03125", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat b = new CFloatNative("0.0001220703125", CFloatNativeAPI.FP_TYPE_SINGLE);

    aI = new CFloatImpl(a.copyWrapper(), a.getType());
    bI = new CFloatImpl(b.copyWrapper(), b.getType());

    CFloat res = aI.subtract(bI);
    assertThat(new CFloatNative(res.copyWrapper(), res.getType()).toString())
        .isEqualTo("12345.0311279296875");

    res = bI.subtract(aI);
    assertThat(new CFloatNative(res.copyWrapper(), res.getType()).toString())
        .isEqualTo("-12345.0311279296875");
  }

  @Test
  public void isZeroTest() {
    CFloat a = new CFloatImpl("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("0.0", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.isZero()).isEqualTo(b.isZero());

    CFloat c = new CFloatImpl("-1.0", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(new CFloatNative(c.copyWrapper(), c.getType()).toString()).isEqualTo("-1.0");

    a = a.multiply(c);
    b = b.multiply(c);

    assertThat(b.toString()).isEqualTo("-0.0");
    assertThat(b.isNegative()).isTrue();
    assertThat(a.isZero()).isEqualTo(b.isZero());
    assertThat(a.isNegative()).isEqualTo(b.isNegative());
  }

  @Test
  public void divisionTest() {
    CFloat a = new CFloatImpl("4", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatImpl("2", CFloatNativeAPI.FP_TYPE_SINGLE);

    CFloat c = a.divideBy(b).divideBy(a);
    c = new CFloatNative(a.divideBy(c).copyWrapper(), a.getType());

    assertThat(c.toString()).isEqualTo("8.0");

    CFloat d = new CFloatImpl("12.5625", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    assertThat(new CFloatNative(d.copyWrapper(), d.getType()).toString()).isEqualTo("12.5625");
  }

  @Test
  public void truncTest() {
    CFloat a = new CFloatImpl("-0.25", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-0.25");

    a = a.trunc();
    b = b.trunc();

    assertThat(a.isZero()).isTrue();
    assertThat(b.isZero()).isTrue();
    assertThat(a.isNegative()).isTrue();
    assertThat(b.isNegative()).isTrue();
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("123.625", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("123.625");

    a = a.trunc();
    b = b.trunc();

    assertThat(b.toString()).isEqualTo("123.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());

    a = new CFloatImpl("12345667", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("12345667.0");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void roundTest() {
    CFloat a = new CFloatImpl("2134.5625", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("2134.5625");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("2135.0");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("-2134.5625", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-2134.5625");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("-2135.0");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("-2134.3125", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("-2134.3125");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("-2134.0");
    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("63.96875", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("63.96875");

    a = a.round();
    b = b.round();

    assertThat(b.toString()).isEqualTo("64.0");

    assertThat(a.copyWrapper().getExponent())
        .isEqualTo(b.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(a.copyWrapper().getMantissa())
        .isEqualTo(b.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());
  }

  @Test
  public void divisionTest_2() {
    CFloat a = new CFloatImpl("625", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatImpl("1000", CFloatNativeAPI.FP_TYPE_DOUBLE);

    CFloat c = new CFloatNative(a.copyWrapper(), a.getType());
    CFloat d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("625.0");
    assertThat(d.toString()).isEqualTo("1000.0");

    CFloat e = a.divideBy(b);
    CFloat f = c.divideBy(d);

    assertThat(f.toString()).isEqualTo("0.625");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("96875", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatImpl("100000", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat one = new CFloatImpl("1", CFloatNativeAPI.FP_TYPE_DOUBLE);

    c = new CFloatNative(a.copyWrapper(), a.getType());
    d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("96875.0");
    assertThat(d.toString()).isEqualTo("100000.0");

    e = one.divideBy(b.divideBy(a));
    one = new CFloatNative(one.copyWrapper(), one.getType());
    f = one.divideBy(d.divideBy(c));

    assertThat(f.toString()).isEqualTo("0.96875");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());

    a = new CFloatImpl("87500", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatImpl("100000", CFloatNativeAPI.FP_TYPE_DOUBLE);

    c = new CFloatNative(a.copyWrapper(), a.getType());
    d = new CFloatNative(b.copyWrapper(), b.getType());

    assertThat(c.toString()).isEqualTo("87500.0");
    assertThat(d.toString()).isEqualTo("100000.0");

    e = a.divideBy(b);
    f = c.divideBy(d);

    assertThat(f.toString()).isEqualTo("0.875");
    assertThat(e.copyWrapper().getExponent())
        .isEqualTo(f.copyWrapper().getExponent() & (b.getExponentMask() ^ b.getSignBitMask()));
    assertThat(e.copyWrapper().getMantissa())
        .isEqualTo(f.copyWrapper().getMantissa() & b.getNormalizedMantissaMask());
  }

  @Test
  public void divisionTest_3() {
    CFloat one = CFloatNativeAPI.ONE_DOUBLE;
    CFloat nOne = new CFloatNative(one.copyWrapper(), one.getType());

    CFloat a = new CFloatImpl("123348857384573888", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("123348857384573888.0");

    a = one.divideBy(a);
    b = nOne.divideBy(b);

    assertThat(b.toString())
        .isEqualTo(
            "0.000000000000000008107087663424604897789104021903747826661400176467657530121613262963364832103252410888671875");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString())
        .isEqualTo(
            "0.000000000000000008107087663424604897789104021903747826661400176467657530121613262963364832103252410888671875");

    a = new CFloatImpl("123", CFloatNativeAPI.FP_TYPE_DOUBLE);
    b = new CFloatNative(a.copyWrapper(), a.getType());

    assertThat(b.toString()).isEqualTo("123.0");

    a = one.divideBy(a);
    b = nOne.divideBy(b);

    assertThat(b.toString())
        .isEqualTo("0.00813008130081300899039131735435148584656417369842529296875");
    assertThat(new CFloatNative(a.copyWrapper(), a.getType()).toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest() {
    CFloat a = new CFloatImpl("2784365.34543", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("2784365.34543", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_floatValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_negativeFloatValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  @Ignore // known to fail
  public void toStringTest_doubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.FP_TYPE_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  @Ignore // known to fail
  public void toStringTest_negativeDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.FP_TYPE_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  @Ignore // known to fail
  public void toStringTest_longDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("0.6", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat b = new CFloatNative("0.6", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  @Ignore // known to fail
  public void toStringTest_negativeLongDoubleValueWithLeadingZero() {
    CFloat a = new CFloatImpl("-0.6", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat b = new CFloatNative("-0.6", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_floatValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void toStringTest_doubleValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.FP_TYPE_DOUBLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.FP_TYPE_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  @Ignore // known to fail
  public void toStringTest_longDoubleValueWithZeroExponent() {
    CFloat a = new CFloatImpl("1.000001", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    CFloat b = new CFloatNative("1.000001", CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);

    assertThat(a.copyWrapper().getExponent()).isEqualTo(b.copyWrapper().getExponent());
    assertThat(a.copyWrapper().getMantissa()).isEqualTo(b.copyWrapper().getMantissa());
    assertThat(a.toString()).isEqualTo(b.toString());
  }

  @Test
  public void castFloatToLongDoubleTest() {
    CFloat a = new CFloatImpl("893473.378465376", CFloatNativeAPI.FP_TYPE_SINGLE);
    CFloat b = new CFloatNative("893473.378465376", CFloatNativeAPI.FP_TYPE_SINGLE);

    assertThat(a.toString()).isEqualTo(b.toString());
    a = a.castTo(CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    b = b.castTo(CFloatNativeAPI.FP_TYPE_LONG_DOUBLE);
    assertThat(a.toString()).isEqualTo(b.toString());
  }
}
