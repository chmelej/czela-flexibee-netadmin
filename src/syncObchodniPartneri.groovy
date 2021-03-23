import groovy.sql.Sql
import net.czela.common.Helper
import net.czela.flexibee.FlexibeeConnector
import net.czela.netadmin.NetadminConnector
import net.czela.netadmin.User

Sql sql = Helper.newSqlInstance("app.properties", this)

def fbc = new FlexibeeConnector()
fbc.initClient(Helper.get("flexibee.server"), Helper.get("flexibee.company"), Helper.get("flexibee.user"), Helper.get("flexibee.password"))

def nac = new NetadminConnector(sql)
nac.dokladBaseDir = Helper.get("netadmin.doklady.dir","/tmp/doklady/")

/*
 Synchronizace Obchodni Partner = Clen spolku
 - pokud je to v AF i netadmin - je to ok a preskakuji
 - pokud je to jen v AF pridam do netadmin
 - pokud je to jen v netadmin pridam do AF
 - porovnavam pouze podle kod - texty se mohou rozchazet (nejsem si jistej ktera hodnota je spravna)
*/

// nactu data z Flexibee
def adresar = toMap(fbc.listAdresarCleni(),'kod');

Map<String, User> membersMap = [:]
// nactu data z Netadmin (vsechny lidi kteri kdy byli cleny od 1.1.2021 do ted)
nac.selectAllMembers().each { User user ->
    String key = userToKod(user)
    membersMap.put(key, user)
}

int cnt = 0;
adresar.each { key, val ->
    User s = membersMap.remove(key)
    if (s == null) {
        println("WARN: Polozka v AF nema zaznam v netadminu!! $val")
        // TODO Tady se zacnou hromadit zablokovany, vylouceny, preruseny a ukonceni clenove
    //} else {
    //    println("OK: nasel jsem clena")
        // TODO porovnej a pripadne uloz zmeny do AF
    }
    cnt++
}

membersMap.each { String key, User val ->
    println "INFO: create a new user $val"
    fbc.postAdresar(userToAdresarMap(val));
    cnt++
}

println "INFO: $cnt clenu je synchronizovano."

static String userToKod(User user) {
    return user.vs as String
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

Map userToAdresarMap(User user) {
    [
            'kod': userToKod(user),
            'nazev': jmenoPrijmeniToNazev(user),
            'nazev2': user.login,
            'mesto': user.mesto,
            'ulice': user.adresa,
            'psc': user.psc,
            'tel': user.telefon,
            'mobil': user.mobil,
            'skupFir': 'code:ČLEN',
            'typVztahuK': ''
    ]
}

String jmenoPrijmeniToNazev(User user) {
    if (user.jmeno != null && user.prijmeni != null)
        return user.jmeno+" "+user.prijmeni
    else if (user.jmeno == null && user.prijmeni == null)
        return user.login
    else
        return user.prijmeni == null?user.prijmeni:user.jmeno
}