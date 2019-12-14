import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.common.Helper.asLong

Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

int cnt=0
int prev=-1
while(prev < cnt) {
    prev = cnt
    sql.eachRow("""SELECT d.id, d.datum_date, d.vs, d.obsah, d.cena, u.jmeno, u.prijmeni, u.adresa, u.mesto, u.psc, u.email
 FROM denik d join users u on u.vs = d.vs where d.md='315000' and d.d='684000' and datum_date >= '2019-01-01' and ifnull(doklad,'') = '' limit 1000""") { row ->
        fbc.genPredpisClenskehoPrispevku(row.DATUM_DATE, asLong(row.VS as String), row.CENA, row.JMENO, row.PRIJMENI, row.ADRESA, row.MESTO, row.PSC, row.EMAIL)
        sql.executeUpdate("UPDATE denik set doklad = 'FLEXIBEE_SENT' where id = ?", [row.id])
        cnt++
    }
}
println("$cnt radku bylo vlozeno do flexibee")
