package net.czela.flexibee

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.czela.flexibee.https.AnyHostVerifier
import net.czela.flexibee.https.DefaultTrustManager

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.DateFormat
import java.text.SimpleDateFormat

import static net.czela.common.Helper.notEmpty


class FlexibeeConnector {
    private def host
    private def company
    private String encodedAuth

    public static final String ID = 'id'
    public static final String LAST_UPDATE = 'lastUpdate'
    public static final String KOD = 'kod'
    public static final String STAV_UHRK = 'stavUhrK'
    public static final String DAT_VYST = 'datVyst'
    public static final String DAT_SPLAT = 'datSplat'
    public static final String SUM_CELKEM = 'sumCelkem'
    public static final String SUM_ZALOHY = 'sumZalohy'
    public static final String SUM_ZALOHY_MENA = 'sumZalohyMen'
    public static final String SUM_CELKEM_MENA = 'sumCelkemMen'
    public static final String ZBYVA_UHRADIT_MENA = 'zbyvaUhraditMen'
    public static final String ZBYVA_UHRADIT = 'zbyvaUhradit'
    public static final String MENA = 'mena'
    public static final String MENA_REF = 'mena@ref'
    public static final String MENA_SHOW_AS = 'mena@showAs'
    public static final String FIRMA = 'firma'
    public static final String FIRMA_REF = 'firma@ref'
    public static final String FIRMA_SHOW_AS = 'firma@showAs'
    public static final String POPIS = 'popis'
    public static final String WINSTROM = 'winstrom'
    public static final String EVIDENCE_FAKTURA_PRIJATA = 'faktura-prijata'
    public static final String EVIDENCE_FAKTURA_VYDANA = 'faktura-vydana'
    public static final String EVIDENCE_FAKTURA_PRIJATA_POLOZKA = 'faktura-prijata-polozka'
    public static final String EVIDENCE_PRILOHA = 'priloha'
    public static final String EVIDENCE_CINNOST = 'cinnost'
    public static final String EVIDENCE_STREDISKO = 'stredisko'

    def initClient(def host, def company, def username, def password) {
        this.host = host
        this.company = company
        String auth = username + ":" + password
        encodedAuth = auth.getBytes(Charset.forName("US-ASCII")).encodeBase64().toString()

        SSLContext ctx = SSLContext.getInstance("TLS")
        def trustManagers = new TrustManager[1]
        trustManagers[0] = new DefaultTrustManager()
        ctx.init(new KeyManager[0], trustManagers, new SecureRandom())
        SSLContext.setDefault(ctx)
    }

    def getJson(def evidence, def detail = null, def params = []) {
        def link
        params.add('auth=http')
        String encodedParams = params.collect({
            def m = it =~ /(.+=)(.+)/
            m[0][1]+urlEncode(m[0][2])
        }).join('&')
        if (detail) {
            link = "${host}/c/${company}/${evidence}/${detail}.json?$encodedParams"
        } else {
            link = "${host}/c/${company}/${evidence}.json?$encodedParams"
        }
        URL apiUrl = new URL(link)

        HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection()
        connection.setRequestMethod("GET")
        connection.setDoOutput(true)
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth)
        connection.setHostnameVerifier(new AnyHostVerifier())
        connection.connect()

        // read the output from the server
        Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))
        def json = new JsonSlurper().parse(reader)
        return json
    }

    def getBytes(def evidence, def detail, def params = []) {
        def link
        params.add('auth=http')
        String encodedParams = params.collect({
            def m = it =~ /(.+=)(.+)/
            m[0][1]+urlEncode(m[0][2])
        }).join('&')
        if (detail) {
            link = "${host}/c/${company}/${evidence}/${detail}/content?$encodedParams"
        } else {
            assert false
        }
        URL apiUrl = new URL(link)

        HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection()
        connection.setRequestMethod("GET")
        connection.setDoOutput(true)
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth)
        connection.setHostnameVerifier(new AnyHostVerifier())
        connection.connect()

        // read the output from the server
        return connection.getInputStream().getBytes()
    }

    def postJson(String evidence, def map) {
        def link = "${host}/c/${company}/${evidence}.json"
        String data = JsonOutput.toJson([ "winstrom": ["$evidence": map ] ])
        URL apiUrl = new URL(link)

        println data
        HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection()
        connection.setHostnameVerifier(new AnyHostVerifier())
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json");
        connection.setAllowUserInteraction(false)
        connection.setDoOutput(true)
        connection.setDoInput(true)
        OutputStream os = connection.getOutputStream()
        try {
            os.write(data.bytes)
            os.flush()
            os.close()
            connection.connect()
            Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            // read the output from the server
            def json = new JsonSlurper().parse(reader)
            return json
        } catch(Exception e) {
            Reader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))
            println "ErrorStream:" + reader.text
            throw e;
        }
    }

    def putJson(String evidence, def map) {
        def link = "${host}/c/${company}/${evidence}.json"
        String data = JsonOutput.toJson([ "winstrom": ["$evidence": map ] ])
        URL apiUrl = new URL(link)

        println data
        HttpsURLConnection connection = (HttpsURLConnection) apiUrl.openConnection()
        connection.setHostnameVerifier(new AnyHostVerifier())
        connection.setRequestMethod("PUT")
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json");
        connection.setAllowUserInteraction(false)
        connection.setDoOutput(true)
        connection.setDoInput(true)
        try {
            OutputStream os = connection.getOutputStream()
            os.write(data.bytes)
            os.flush()
            os.close()
            connection.connect()
            Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))


            // read the output from the server
            def json = new JsonSlurper().parse(reader)
            return json
        } catch(Exception e) {
            Reader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))
            println "ErrorStream:" + reader.text
            throw e;
        }
    }

    List listPrijateFaktury() {
        def params = [
                'detail=custom:lastUpdate,kod,typDokl,cisDosle,stavUhrK,datSplat,sumCelkem,varSym,firma,popis,primUcet,protiUcet,stredisko,cinnost,typDoklBan,pocetPriloh,bezPolozek,banSpojDod,buc,smerKod',
                'limit=300',
                ]
        def json = getJson(EVIDENCE_FAKTURA_PRIJATA, null, params)

        def list = []
        json[WINSTROM][EVIDENCE_FAKTURA_PRIJATA].each { it -> list.add(it) }

        return list
    }

    Map listPrijateFakturyPolozky() {
        def params = [
                'detail=custom:doklFak,lastUpdate,nazev,cenaMj,dphDalUcet,dphMdUcet,mnozMj,objem,stredisko,nazev',
                'limit=10000',
        ]
        def json = getJson(EVIDENCE_FAKTURA_PRIJATA_POLOZKA, null, params)

        def map = new HashMap<String, List>()
        json[WINSTROM][EVIDENCE_FAKTURA_PRIJATA_POLOZKA].each { it ->
            String key = it['doklFak'].replaceFirst(/^code:/,'')
            List items = map.get(key)
            if (items == null) {
                items = []
                map.put(key, items)
            }
            items.add(it)
        }
        return map
    }

    Map listPrilohy() {
        def params = [
                'detail=custom:contentType,dataSize,doklFak,lastUpdate',
                //content,
                'limit=10000',
        ]
        def json = getJson(EVIDENCE_PRILOHA, null, params)

        def map = new HashMap<String, List>()
        json[WINSTROM][EVIDENCE_PRILOHA].each { it ->
            String key = it['doklFak'].replaceFirst(/^code:/,'')
            List items = map.get(key)
            if (items == null) {
                items = []
                map.put(key, items)
            }
            items.add(it)
        }
        return map
    }

    /**
     * pokud je priloha velika (26MB) tak stahovani pres json pada :-/
     * @param id
     * @return
     */
    byte[] getPriloha(def id) {
        if (id != null) {
            return getBytes(EVIDENCE_PRILOHA, id )
        }
        return null
    }

    def urlEncode(String param) {
        URLEncoder.encode(param, StandardCharsets.UTF_8.toString())
    }

    def static asDate(String dateString) {
        if (notEmpty(dateString)) {
            DateFormat fmt
            dateString = dateString.replaceFirst(/\+\d\d:\d\d$/,'')
            if (dateString.contains('T')) {
                fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            } else {
                fmt = new SimpleDateFormat("yyyy-MM-dd")
            }
            return fmt.parse(dateString)
        }
        return null
    }

    static def parseCisloUctu(String banSpojDod) {
        if (notEmpty(banSpojDod)) {
            def m = banSpojDod =~ /(\d+\-\d+\/\d+)/
            if (m.find()) {
                return m[0][1]
            } else {
                m = banSpojDod =~ /(\d+\/\d+)/
                if (m.find()) {
                    return m[0][1]
                }
            }
        }
        return null
    }

    def genPredpisClenskehoPrispevku(Date datum, Long vs, BigDecimal cena, String jmeno, String prijmeni, String adresa, String mesto, String psc, String email) {
        def cal = new GregorianCalendar()
        cal.setTime(datum)
        int rok = cal.get(Calendar.YEAR)
        def mesic = 1 + cal.get(Calendar.MONTH)
        def nextmesic = mesic + 1
        int nextrok = rok
        if (nextmesic > 12) {
            nextmesic = 1; nextrok++
        }
        def map = [
                "typDokl": "code:FAKTURA",
                "popis": "$jmeno $prijmeni - předpis členského příspěvku za $rok/${fmtMonth(mesic)}",
                "datVyst":	"${rok}-${fmtMonth(mesic)}-01+01:00",
                "datSplat": "${nextrok}-${fmtMonth(nextmesic)}-15+01:00",
                "nazFirmy": "$jmeno $prijmeni",
                "ulice": coalesce(adresa,'-'),
                "mesto": coalesce(mesto,'-'),
                "psc": coalesce(psc,""),
                "stat": "code:CZ",
                "bezPolozek": "true",
                "clenDph": "code:000U",
                "varSym": "$vs",
                "sumOsv": "$cena",
                "sumCelkem": "$cena",
                "typUcOp": 'code:CLENSKE_PRISPEVKY',
                "primUcet":	"code:315000",
        ]
        def json = postJson(EVIDENCE_FAKTURA_VYDANA, map)
        /**
         * {"winstrom":{"@version":"1.0",
         *      "success":"true",
         *      "stats":{"created":"0","updated":"1","deleted":"0","skipped":"0","failed":"0"},
         *      "results":[{"id":"714","request-id":"714","ref":"/c/czela_net_z_s_1/faktura-vydana/714.json"}]}}
         * {"winstrom":{"@version":"1.0",
         *      "success":"false",
         *      "stats":{"created":"0","updated":"0","deleted":"0","skipped":"0","failed":"1"},
         *      "results":[{"errors":[{"message":"Pole 'Interní číslo' musí být vyplněno. [DoklFak -1]","for":"kod","path":"faktura-vydana[temporary-id=null].kod","code":"INVALID"}]}]}}
         */
    }

    def postCinnost(def kod, def nazev) {
        assert kod != null;
        assert nazev != null;

        def json = postJson(EVIDENCE_CINNOST, ['kod':kod, 'nazev': nazev])
    }

    def postStredisko(def kod, def nazev) {
        assert kod != null;
        assert nazev != null;

        def json = postJson(EVIDENCE_STREDISKO, ['kod':kod, 'nazev': nazev])
    }

    def listCinnosti() {
        def params = [
                'detail=custom:kod,nazev',
                'limit=10000',
        ]
        def json = getJson(EVIDENCE_CINNOST, null, params)

        def list = []
        json[WINSTROM][EVIDENCE_CINNOST].each { it -> list.add(it) }

        return list
    }

    def listStrediska() {
        def params = [
                'detail=custom:kod,nazev',
                'limit=10000',
        ]
        def json = getJson(EVIDENCE_STREDISKO, null, params)

        def list = []
        json[WINSTROM][EVIDENCE_STREDISKO].each { it -> list.add(it) }

        return list
    }

    static def coalesce(def a, def b) {
        a!=null?a:b
    }

    static String fmtMonth(def m) {
        String.format("%02d", m)
    }
}



