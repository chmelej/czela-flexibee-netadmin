import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*

Sql sql = Helper.newSqlInstance("app.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))


def doklady = [:]
sql.eachRow("""SELECT d.id, concat('code:AKCE:',d.akce) as cinnost, concat('code:SEKCE:',a.sekceid) as stredisko, d.stav 
FROM doklady d
JOIN akce a ON d.akce = a.id AND d.datum_splatnosti > '2020-06-01' AND d.stav > 1 """.toString()) { row ->
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
            String valF = faktura[key]
            String valD = doklad.get(key)
            if (valD != null && (valF == null ||  valD != valF)) {
                changes.put(key, valD)
            }
        }
        if (changes.size() > 0) {
            try {
                ['id', 'typDokl'].each { key -> // kopiruj nezbytne parametry
                    if (faktura.get(key)) {
                        changes.put(key, faktura.get(key))
                    }
                }

                fbc.putJson(EVIDENCE_FAKTURA_PRIJATA, changes);
                println "update ${faktura.get('id')} ... OK"
            } catch(Exception e) {
                println "update ${faktura.get('id')} ... "+e.getMessage()
            }
        }
    }
}
