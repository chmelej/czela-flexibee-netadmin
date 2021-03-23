import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector
/*
 V tabulce cleni_prispevky jsou vygenarovane prispevky pro kazdeho clena, skript tuto tabulku projde a nove radky posle
 do AF kde vytvori pro kazdy "Vydanou fakturu".
 */
Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def now = new Date()
def nowts = now.toTimestamp()
int cnt=0
int prev=-1
while(prev < cnt) {
    prev = cnt
    sql.eachRow("SELECT id, vs, castka, popisek, datum_splatnosti FROM cleni_prispevky" +
            " WHERE datum_vystaveni_dokladu is null OR ucto_doklad_id is null LIMIT 1000") { row ->
        def documentId = fbc.genPredpisClenskehoPrispevku( row.VS as int, row.CENA as BigDecimal, row.POPISEK as String, now, row.DATUM_SPLATNOSTI as Date)
        sql.executeUpdate("UPDATE cleni_prispevky SET ucto_doklad_id = ?, datum_vystaveni_dokladu = ? WHERE id = ?".toString(), [documentId, nowts, row.ID])
        cnt++
    }
}
println("$cnt VF bylo vlozeno do flexibee")
