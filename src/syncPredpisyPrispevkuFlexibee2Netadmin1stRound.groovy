import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static net.czela.flexibee.FlexibeeConnector.EVIDENCE_FAKTURA_VYDANA
import static net.czela.flexibee.FlexibeeConnector.WINSTROM

Sql sql = Helper.newSqlInstance("app.properties", this)
def fmt = new SimpleDateFormat("yyyy-MM-dd")

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

/*
 1. Faze synchronizace predpokladam ze data ve AF vznikly jinou cestou, cili pokusim se je naparovat s daty v tabulce cleni_prispevky
 nejdriv musim najit ID
 pak me zajima jestli je doklad zaplaceny
 dotazy musi byt rozumne, kazdy mesic se vygeneruje pres 1500 dokladu, takze je dobre iterovat po mesici.

 2. Faze vypnu stary generovani dokladu a nahradim timto, soucasti bude ukladani ucto_doklad_id
 potom nemusim hledat vsechny ale jen ty co mi chybi (nejsou zaplaceny) treba od zacatku roku

 3. Faze odladime a muzeme odpojovat na zaklade techto dat, a pak se muzeme vykazslat na denik!

 Pozorovani
 Lidi co maji PHP nemaji zaplaceno nic!
*/

def params = [
        'detail=custom:id,kod,typDokl,datVyst,popis,varSym,zbyvaUhradit',
        'limit=5000',
]

Calendar cal = new GregorianCalendar()
cal.set(Calendar.MILLISECOND, 0)

[1,2,3].each { month ->
    cal.set(2021, month - 1 , 1, 0, 0, 0)
    String firstDayOfMonth = fmt.format(cal.getTime())
    cal.add(Calendar.MONTH,1)
    cal.add(Calendar.DAY_OF_MONTH, -1)
    String lastDayOfMonth = fmt.format(cal.getTime())

    println("Scan VF form $firstDayOfMonth to $lastDayOfMonth")
    String filter = "(datVyst between '$firstDayOfMonth' '$lastDayOfMonth')"

    def json = fbc.getJson(EVIDENCE_FAKTURA_VYDANA, filter, params)
    int docCnt = 0, updCnt = 0, nullCnt = 0, errCnt = 0, err2Cnt = 0, zplCnt = 0
    json[WINSTROM][EVIDENCE_FAKTURA_VYDANA].each { it ->
        docCnt++
        def dokladId = it['id']
        def vs = it['varSym']
        def zbyvaUhradit = it['zbyvaUhradit']

        assert dokladId ==~ /^\d+$/
        assert vs ==~ /^\d+$/
        assert zbyvaUhradit ==~ /^[0-9.]+$/

        def m = it['popis'] =~ /(20\d+)\/0?(\d+)/

        if (m.find()) {
            int yyyy = m[0][1] as int
            int mm = m[0][2] as int
            cal.set(yyyy as int, mm - 1, 1, 0, 0, 0)
            Timestamp ts = cal.getTime().toTimestamp()
            def prispevek = sql.firstRow("SELECT id, ucto_doklad_id, datum_zaplaceni FROM cleni_prispevky WHERE vs = ? AND datum_prispevku = ?".toString(), [vs, ts])
            if (prispevek != null) {
                if (prispevek.UCTO_DOKLAD_ID == null) {
                    sql.executeUpdate("UPDATE cleni_prispevky SET ucto_doklad_id = ?, datum_vystaveni_dokladu = datum_prispevku WHERE id = ?".toString(), [dokladId, prispevek.ID])
                    updCnt++
                    if (Double.parseDouble(zbyvaUhradit) == 0 && prispevek.DATUM_ZAPLACENI == null) { // je to zaplaceny
                        sql.executeUpdate("UPDATE cleni_prispevky SET datum_zaplaceni = datum_splatnosti WHERE id = ?".toString(), [prispevek.ID])
                        zplCnt++
                    }
                } else if (prispevek.UCTO_DOKLAD_ID != dokladId) {
                    errCnt++
                } else {
                    if (Double.parseDouble(zbyvaUhradit) == 0 && prispevek.DATUM_ZAPLACENI == null) { // je to zaplaceny
                        sql.executeUpdate("UPDATE cleni_prispevky SET datum_zaplaceni = datum_splatnosti WHERE id = ?".toString(), [prispevek.ID])
                        zplCnt++
                    }
                }
            } else {
                nullCnt++
            }
        } else {
            println("Nenasel ${dokladId}")
            err2Cnt++
        }
        if (docCnt % 15 == 0) print(".")
        if (docCnt % 150 == 0) print("+")
    }
    println ("\ndocCnt = $docCnt, updCnt = $updCnt, nullCnt = $nullCnt, zplCnt = $zplCnt, errCnt = $errCnt, err2Cnt = $err2Cnt")
}
