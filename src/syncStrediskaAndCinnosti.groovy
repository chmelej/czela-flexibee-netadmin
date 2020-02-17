import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector
import net.czela.netadmin.Akce
import net.czela.netadmin.NetadminConnector
import net.czela.netadmin.Sekce

Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def nac = new NetadminConnector(sql)
nac.dokladBaseDir = Helper.get("netadmin.doklady.dir","/tmp/doklady/")

/*
 Synchronizace Sekci (=Stredisko) a Akci(=Cinnost)
 - pokud je to v AF i netadmin - je to ok a preskakuji
 - pokud je to jen v AF pridam do netadmin
 - pokud je to jen v netadmin pridam do AF
 - porovnavam pouze podle kod - texty se mohou rozchazet (nejsem si jistej ktera hodnota je spravna)
*/

// nactu data z Flexibee
def strediska = toMap(fbc.listStrediska(),'kod');
def cinnosti = toMap(fbc.listCinnosti(), 'kod');

def sekceMap = [:] // toMap(nac.selectSekce(),'id');
def akceMap = [:] //toMap(nac.selectAkceByYear(2019), 'id');

//nactu data z Netadmin
nac.selectSekce().each { Sekce sekce ->
    String key = sekceIdToKod(sekce.id)
    sekceMap.put(key, sekce)
}

nac.selectAkceByYear(2019).each { Akce akce ->
    String key = akceIdToKod(akce.id)
    akceMap.put(key, akce)
}

nac.selectAkceByYear(2020).each { Akce akce ->
    String key = akceIdToKod(akce.id)
    akceMap.put(key, akce)
}

// porovnam a srovnam
strediska.each {key, val ->
    def s = sekceMap.remove(key)
    if (s == null) {
        def m = val.kod =~ /SEKCE:(\d+)/
        if (m.matches()) {
            Long id = Long.parseLong(m[0][1])
            def sekce = new Sekce(id: id, nazev: val.nazev, group:'AF');
            nac.upsertSekce(sekce);
        } else {
            println("WARN: stredisko '${val.kod}' nelze vlozit do Netadmin")
        }
    }
}

sekceMap.each {String key, Sekce val ->
    def s = strediska.remove(key)
    if (s == null) {
        fbc.postStredisko(sekceIdToKod(val.id), val.nazev)
    }
}

cinnosti.each {key, val ->
    Akce a = akceMap.remove(key) as Akce
    if (a == null) {
        def m = val.kod =~ /AKCE:(\d+)/
        if (m.matches()) {
            Long id = Long.parseLong(m[0][1])
            def akce = new Akce(id: id, nazev: val.nazev);
            try {
                nac.insertAkce(akce);
            } catch (Exception e) {
                println("WARN: cinnosti '${val.kod}' nelze vlozit do Netadmin akce.id = $id :"+e.getMessage())
            }
        } else {
            println("WARN: cinnosti '${val.kod}' nelze vlozit do Netadmin")
        }
    }
}

akceMap.each {String key, Akce val ->
    def s = cinnosti.remove(key)
    if (s == null) {
        fbc.postCinnost(akceIdToKod(val.id), val.nazev)
    }
}

static String akceIdToKod(long akceId) {
    return "AKCE:$akceId" as String
}

static toMap(List list, String keyName) {
    def map = [:]
    list.each { itemMap ->
            def itemKey = (itemMap instanceof Map)?
                    itemMap.get(keyName):
                    itemMap.invokeMethod(getter(keyName),null)
            map.put(itemKey, itemMap)
    }
    return map;
}

static String getter(String str) {
    if(str == null || str.isEmpty()) {
        return str;
    }

    return "get"+str.substring(0, 1).toUpperCase() + str.substring(1);
}

private static String sekceIdToKod(long id) {
    "SEKCE:${id}".toString()
}

