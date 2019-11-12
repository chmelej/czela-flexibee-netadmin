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

Sql sql = Helper.newSqlInstance("app-test.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

sql.eachRow("""SELECT d.datum_date, d.vs, d.obsah, d.cena, u.jmeno, u.prijmeni, u.adresa, u.mesto, u.psc, u.email
 FROM denik d join users u on u.vs = d.vs where d.md='315000' and d.d='684000' and datum_date >= '2019-01-01' and datum_date < '2019-02-01' limit 10""") { row ->
    fbc.genPredpisClenskehoPrispevku(row.DATUM_DATE, asLong(row.VS as String), row.CENA, row.JMENO, row.PRIJMENI, row.ADRESA, row.MESTO, row.PSC, row.EMAIL)
}
