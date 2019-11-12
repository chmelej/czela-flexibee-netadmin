import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector
import net.czela.netadmin.Doklad
import net.czela.netadmin.NetadminConnector

import java.math.RoundingMode

import static net.czela.common.Helper.asDecimal
import static net.czela.common.Helper.asLong
import static net.czela.common.Helper.filterNumbersOnly
import static net.czela.common.Helper.notEmpty
import static net.czela.flexibee.FlexibeeConnector.asDate
import static net.czela.flexibee.FlexibeeConnector.parseCisloUctu
import static net.czela.netadmin.NetadminConnector.DOK_FAKTURA
import static net.czela.netadmin.NetadminConnector.DOK_STAV_NEPRIRAZENY
import static net.czela.netadmin.NetadminConnector.DOK_STAV_PROPLACENY
import static net.czela.netadmin.NetadminConnector.DOK_UCTENKA
import static net.czela.netadmin.NetadminConnector.DOK_UNKNOWN



Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def nac = new NetadminConnector(sql)
nac.dokladBaseDir = Helper.get("netadmin.doklady.dir","/tmp/doklady/")

// nactu data z Flexibee
def faktury = fbc.listPrijateFaktury()
def polozky = fbc.listPrijateFakturyPolozky()
def prilohy = fbc.listPrilohy()

// nactu data z netadminu
def ids = faktury.collect({it['kod']})
def doklady = nac.selectDokladyByIds(ids)
def rozpisy = nac.selectRozpisyByDokladyIds(ids)

def dokladyMap = [:]
doklady.each { dok ->
    def r = rozpisy?.get(dok.id)
    if (listNotEmpty(r)) {
        dok.rozpisy.addAll(r)
    }
    dokladyMap.put(dok.id, dok)
}

faktury.each() { faktura ->
    def kodFak = faktura[FlexibeeConnector.KOD]
    def polozkyFaktury = polozky.get(kodFak)
    def prilohyFaktury = prilohy.get(kodFak)
    
    if (listNotEmpty(polozkyFaktury)) {
        BigDecimal totalCena = new BigDecimal(0)
        polozkyFaktury.each { polozka ->
            def cena = asDecimal(polozka['cenaMj'])
            def mnozstvi = asDecimal(polozka['mnozMj'])
            if (mnozstvi != null) {
                cena = cena.multiply(mnozstvi)
            }
            if (cena != null) { totalCena = totalCena.add(cena) }
            //println("DEBUG: $kodFak - polozka $cena $polozka")
        }
        def sumCelkem = asDecimal(faktura['sumCelkem'])
        sumCelkem = sumCelkem.setScale(2, RoundingMode.HALF_UP)
        totalCena = totalCena.setScale(2, RoundingMode.HALF_UP)
        if (totalCena.compareTo(sumCelkem) != 0) {
            println("WARN: Prijata faktura $kodFak celkova suma $sumCelkem neodpovida sume jednotlivých položek $totalCena")
        }
    }

    if (listNotEmpty(prilohyFaktury)) {
        def pdf = 0
        prilohyFaktury.each { priloha ->
            if (priloha['contentType'] == 'application/pdf') { pdf++ }
        }

        if (pdf != 1) {
            println("WARN: Prijata faktura $kodFak v prilohach jsem nenasel naskenovany doklad!($pdf/${prilohyFaktury.size()})")
        } else {
            // vim ze je prave jeden doklad
            /*
            prilohyFaktury.each { priloha ->
                if (priloha['contentType'] == 'application/pdf') {
                    byte[] blob = fbc.getPriloha(priloha['id'])
                    if (blob.size() > 1000)
                        nac.storeDokladOnFS(kodFak, blob)
                    else
                        println("WARN: Prijata faktura $kodFak naskenovany doklad je mensi nez 1000 znaku!)")
                }
            }
            */
        }
    } else {
        println("WARN: Prijata faktura $kodFak neobsahuje prilohu s naskenovanym dokladem!")
    }

    Doklad dok = convertFaktura2Doklad(faktura, polozkyFaktury, prilohyFaktury)

    Doklad netadminDoklad = dokladyMap.get(dok.id)

    try {
        if (netadminDoklad == null) {
            nac.insertDoklad(dok)
//    } else if (notEquals(dok, netadminDoklad)) {
//        nac.updateDoklad(dok, netadminDoklad)
        //} else {
        //    println("Doklad $dok.id je v pořádku a můžeme ho přeskočit.")
        }
    } catch(Exception e) {
        println(dok)
        throw e
    }
}

Doklad convertFaktura2Doklad(def faktura, def polozky, def prilohy) {
    def dok = new Doklad(
    id: faktura['kod'],
    datum: asDate(faktura['lastUpdate']),
    datumSplatnosti: asDate(faktura['datSplat']),
    //akce: faktura[''],
    dodavatel: faktura['firma'],
    ucet: parseCisloUctu(faktura['banSpojDod@showAs']),
    vs: asLong(filterNumbersOnly(faktura['varSym'])),
    cena: asDecimal(faktura['sumCelkem']),
    //komu: faktura[''],
    stav: notEmpty(faktura['stavUhrK'])? DOK_STAV_PROPLACENY: DOK_STAV_NEPRIRAZENY,
    obsah: faktura['popis'],
    poznamka: 'import z Abra.Flexibee',
    //doctype: faktura[''],
    )

    dok.doctype = dok.id.startsWith("PF")?DOK_FAKTURA:dok.id.startsWith("ÚČT")?DOK_UCTENKA:DOK_UNKNOWN

    if (listNotEmpty(polozky)) {
        polozky.each { polozka ->
            // id dokladid cena vs obsah umisteniid souvztaznostid
            Integer id
            String dokladId = dok.id
            BigDecimal cena
            Integer vs
            obsah = polozka['nazev']
            //Integer umisteniId
            //Integer souvztaznostId
            // ,lastUpdate,,cenaMj,dphDalUcet,dphMdUcet,mnozMj,objem,stredisko',

        }
        // TODO } else { // kazdy doklad musi mit alepon jednu polozku - takze kdyz tam neni tak si ji vygenerujeme
    }

    return dok

    // primUcet,protiUcet,stredisko,zakazka,typDoklBan,pocetPriloh,bezPolozek',
}

def listNotEmpty(List l) {
    return  l != null && l.size() > 0
}
