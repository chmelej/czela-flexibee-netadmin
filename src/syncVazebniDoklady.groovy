import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector
import net.czela.netadmin.NetadminConnector

/**
 * Pokud jsou v netaminu nejaky doklady bez akce, tak se kouknu do AF jestli doklad neni spojen s
 * jinym dokladem ktery akci ma. coz by melo pomoc pri vyplnovani akci u dokladu.
 */

Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def nac = new NetadminConnector(sql)
nac.dokladBaseDir = Helper.get("netadmin.doklady.dir","/tmp/doklady/")

sql.eachRow("SELECT id FROM doklady where akce = 0 and id not rlike '^S[0-9]+\$'  order by STR_TO_DATE(datum ,'%d.%m.%Y') desc".toString()) { row ->
    boolean success = false
    def id = row.ID as String
    def json = fbc.listVazebniDoklady(FlexibeeConnector.EVIDENCE_FAKTURA_PRIJATA, id)
    def relatedIds = []
    json.each { vdoc ->
        def rid = vdoc.kod as String
        if (rid != id) {
            relatedIds.add(rid)
        }
    }
    if (relatedIds.size() > 0) {
        //println(relatedIds)
        String cond = relatedIds.collect({"?"}).join(", ")
        def relatedAkce = []
        sql.eachRow("SELECT akce FROM doklady WHERE akce > 0 AND id in ($cond)".toString(), relatedIds) { row2 ->
            relatedAkce.add(row2.AKCE as Long)
        }
        if (relatedAkce.size() == 1) {
            Long akceId = relatedAkce[0] as Long
            println("Nasel jsem akci ${akceId} k dokladu $id")
            sql.executeUpdate("UPDATE doklady SET akce = ? WHERE id = ?".toString(), [akceId, id])
            success = true
        }
    }
    if (!success) {
        println("nepodarilo se najit vazbu pro doklad $id")
    }
}