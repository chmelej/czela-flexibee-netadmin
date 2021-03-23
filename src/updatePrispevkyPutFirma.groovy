import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*

/*  Jednorazovy skript ktery se snazi opravit data, drive jsme vkladali Faktury na prispevek a firma byl jen generovany
    text, ted mame cleny v AF jako obchodni partnery v adresari a Faktura musi obsahovat jejich kod. cili snahou je to
    projit a kde to je mozne to nahradit. Bohuzel u proplacenych (sparovanych a zauctovanych) faktur to nelze menit.

    novou metodu bychom radi nasadily od 1.1.2021
 */


Sql sql = Helper.newSqlInstance("app.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def params = [
        'detail=custom:kod,typDokl,datVyst,popis,firma,varSym',
        'limit=10000',
]

int tot=0
int upd=0

def filter = "(datVyst GTE '2021-01-01' AND zbyvaUhradit GTE 1)" // najde vsechny neuhrazene
def json = fbc.getJson(EVIDENCE_FAKTURA_VYDANA, filter, params)
json[WINSTROM][EVIDENCE_FAKTURA_VYDANA].each { it ->
    def vs = it['varSym']
    if (it['popis'] ==~ /.*předpis členského příspěvku.*/ && vs ==~ /^\d+$/ && !(it['firma'] as String)?.startsWith('code:')) {
        println(it)
        def map = [
                "id": it['id'],
                "typDokl": it['typDokl'],
                "firma": "code:$vs",
        ]
        fbc.putJson(EVIDENCE_FAKTURA_VYDANA, map)
        upd++
    }
    tot++;
}

print "updated $upd/$tot row(s)"
