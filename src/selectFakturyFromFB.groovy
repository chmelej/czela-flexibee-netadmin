import net.czela.flexibee.FlexibeeConnector

import static net.czela.flexibee.FlexibeeConnector.*



def fc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))
def json = fc.getJson('faktura-prijata')
json[WINSTROM][EVIDENCE_FAKTURA_PRIJATA].each { faktura ->
    println faktura
}
