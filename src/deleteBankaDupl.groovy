import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*

Sql sql = Helper.newSqlInstance("app.properties", this)
def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def f = new File("/home/chmelej/duplicitni_doklady.txt")

f.eachLine { line ->
    println(line)
    deleteBanka(fbc, line); //"B-0001/2019");
}

void deleteBanka(FlexibeeConnector fbc, String kod) {
    final String BANKA = 'banka'

    def params = [
            'detail=custom:kod,typDokl',
            'limit=10',
    ]
    kod = kod.replaceAll('/','%2f').replaceAll('\\+','%2b').trim()
    def doklad = null;
    try {
        json = fbc.getJson(BANKA, "code:$kod", params)
        def doklady = json[WINSTROM][BANKA]
        if (doklady != null && doklady.size() == 1) {
            doklad = doklady[0];
        }
    } catch (Exception e) {
        // doklad tam asi neni
    }

    if (doklad != null) {
        map = [
                'id': doklad['id'],
                'typDokl': doklad['typDokl'],
                '@action': 'delete',
        ]
        def del = fbc.postJson(BANKA, map)
        println(del)
    } else {
        println("Neco je spatne $json")
    }
}