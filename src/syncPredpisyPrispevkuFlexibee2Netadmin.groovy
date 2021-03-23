import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.EVIDENCE_FAKTURA_VYDANA
import static net.czela.flexibee.FlexibeeConnector.WINSTROM

/*
 V netadmin.cleni_prispevky si najdu nezaplacene (nezparovane) VF a projdu je v AF jestli uz se neprobehla platba a parovani.
 Pokud ano ulozim si do netadmin.cleni_prispevky dnesni datum.
 */
Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

int maxItems = 200
def params = [
        'detail=custom:id',
        "limit=$maxItems",
]

def listOfRows = sql.rows("SELECT id, ucto_doklad_id FROM cleni_prispevky where datum_zaplaceni is null AND ucto_doklad_id is not null".toString())
def listOfListOfIds = listOfRows.collect({it.UCTO_DOKLAD_ID as String }).collate(maxItems)
HashMap<String, Long> reverseMap = new HashMap<String, Long>()
listOfRows.each { reverseMap.put(it.UCTO_DOKLAD_ID as String, it.ID as Long) }

int okCnt = 0
listOfListOfIds.each { listOfId ->
    def inCondition = listOfId.join(",")
    def filter = "(id in ($inCondition) AND zbyvaUhradit = 0)"
    def json = fbc.getJson(EVIDENCE_FAKTURA_VYDANA, filter, params)
    json[WINSTROM][EVIDENCE_FAKTURA_VYDANA].each { it ->
        def uctoId = it['id']
        assert uctoId != null
        Long id = reverseMap.get(uctoId)
        assert id != null
        sql.executeUpdate("UPDATE cleni_prispevky SET datum_zaplaceni = now() WHERE id = ?".toString(), [id])
        okCnt++
    }
}
println("Nasel jsem $okCnt zaplacenych z celkem ${listOfRows.size()} nezaplaceny dokladu.")