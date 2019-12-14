import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*

Sql sql = Helper.newSqlInstance("app.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))


def doklady = [:]
sql.eachRow("""SELECT d.id, concat('code:AKCE:',d.akce) as cinnost, concat('code:SEKCE:',a.sekceid) as stredisko FROM doklady d
join akce a on d.akce = a.id
where d.stav in(2,4,6)""".toString()) { row ->
    Map map = [
            'kod': row.ID,
            'stredisko': row.stredisko,
            'cinnost': row.cinnost,
    ]
    doklady.put(row.ID, map)
}

def faktury = fbc.listPrijateFaktury();

faktury.each { Map faktura ->
    def kod = faktura['kod']
    Map doklad = doklady.get(kod)
    Map changes = [:];
    if (doklad != null) {
        ['cinnost', 'stredisko'].each { key ->
            if (! faktura[key]) {
                changes.put(key, doklad.get(key))
            }
        }
        if (changes.size() > 0) {
            ['id','typDokl'].each { key -> // kopiruj nezbytne parametry
                if (faktura.get(key)) {
                    changes.put(key, faktura.get(key))
                }
            }

            fbc.putJson(EVIDENCE_FAKTURA_PRIJATA, changes);
        }
    }
}
