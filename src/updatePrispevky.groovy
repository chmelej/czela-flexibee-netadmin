import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*

Sql sql = Helper.newSqlInstance("app.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def params = [
        'detail=custom:kod,typDokl,datVyst,popis', //,typUcOp,primUcet',
        'limit=10000',
]

int tot=0
int upd=0

def json = fbc.getJson(EVIDENCE_FAKTURA_VYDANA, null, params)
json[WINSTROM][EVIDENCE_FAKTURA_VYDANA].each { it ->
        /*
        if (! it['typUcOp']) {
                //println(it)
                def map = [
                        "id": it['id'],
                        "typDokl": it['typDokl'],
                        "typUcOp": 'code:CLENSKE_PRISPEVKY',
                ]
                fbc.putJson(EVIDENCE_FAKTURA_VYDANA, map)
        }

        if (! it['primUcet']) {
                println(it)
                def map = [
                        "id": it['id'],
                        "typDokl": it['typDokl'],
                        "primUcet":	"code:315000",
                ]
                fbc.putJson(EVIDENCE_FAKTURA_VYDANA, map)
        }
         */
    def m = it['popis'] =~ /(2019)\/(\d+)/
    if (m.find()) {
        def yyyy = m[0][1]
        def mm = m[0][2]
        String d = "${yyyy}-${mm}-01+01:00"

        if (it['datVyst'] != d) {
            //println(it)
            def map = [
                    "id": it['id'],
                    "typDokl": it['typDokl'],
                    "datVyst":	d,
            ]
            fbc.putJson(EVIDENCE_FAKTURA_VYDANA, map)
            upd++
        }
    }
    tot++;
}

print "updated $upd/$tot row(s)"
