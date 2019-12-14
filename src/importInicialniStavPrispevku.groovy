import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.common.Helper.asLong
import static net.czela.flexibee.FlexibeeConnector.*

def genNedoplatekClenskehoPrispevku(def fbc, Long vs, BigDecimal cena, String jmeno, String prijmeni, String adresa, String mesto, String psc) {
    def map = [
            "typDokl"   : "code:FAKTURA",
            "popis"     : "$jmeno $prijmeni - Nedoplatek2 členských příspěvků evidovany k 31.12.2018",
            "datVyst"   : "2018-12-01+01:00",
            "datSplat"  : "2018-12-01+01:00",
            "nazFirmy"  : "$jmeno $prijmeni",
            "ulice"     : coalesce(adresa, '-'),
            "mesto"     : coalesce(mesto, '-'),
            "psc"       : coalesce(psc, ""),
            "stat"      : "code:CZ",
            "bezPolozek": "true",
            "clenDph"   : "code:000U",
            "varSym"    : "$vs",
            "sumOsv"    : "$cena",
            "sumCelkem" : "$cena",
            "primUcet"  : "code:395000",
            "protiUcet" : "code:395000",
    ]
    fbc.postJson(EVIDENCE_FAKTURA_VYDANA, map)
}

Sql sql = Helper.newSqlInstance("app-test.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

String query = """SELECT d.*, u.jmeno, u.prijmeni, u.adresa, u.mesto, u.psc, u.email FROM (
    SELECT vs, sum(cena) as dluh FROM (
        SELECT vs, sum(cena) as cena FROM denik where md = '315000' and d not in ('961000','962000') and datum_date < '2019-01-01' and vs > 999 and vs < 99999 GROUP by vs
        union all
        SELECT vs, - sum(cena) as cena FROM denik where d = '315000' and md not in ('961000','962000') and datum_date < '2019-01-01' and vs > 999 and vs < 99999 GROUP by vs
    ) s group by vs ) d join users u on u.vs = d.vs where d.dluh > 0 limit 1"""

int cnt = 0;
sql.eachRow(query) { row ->
    genNedoplatekClenskehoPrispevku(fbc, asLong(row.VS as String), row.DLUH, row.JMENO, row.PRIJMENI, row.ADRESA, row.MESTO, row.PSC)
    cnt++
}
println("$cnt rows imported")

