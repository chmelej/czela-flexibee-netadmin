package net.czela.netadmin

//@Grapes(
//        @Grab(group='org.mariadb.jdbc', module='mariadb-java-client', version='2.5.1')
//)

import groovy.sql.Sql
import groovy.transform.builder.InitializerStrategy

import java.text.SimpleDateFormat

import static net.czela.common.Helper.asDecimal
import static net.czela.common.Helper.notEmpty

class NetadminConnector {
    Sql sql
    String dokladBaseDir = "."

    public static final int DOK_STAV_ARCHIV =	8
    public static final int DOK_STAV_PROPLACENY =	7
    public static final int DOK_STAV_KESTRAZCI =	6
    public static final int DOK_STAV_USKRBLIKA =	3
    public static final int DOK_STAV_UOURADY =	5
    public static final int DOK_STAV_KOURADOVI =	4
    public static final int DOK_STAV_KESKRBLIKOVI =	2
    public static final int DOK_STAV_PRIRAZENY =	1
    public static final int DOK_STAV_NEPRIRAZENY =	0

    public static final int DOK_FAKTURA =	1	//faktura
    public static final int DOK_SMLOUVA =	3	//smlouva
    public static final int DOK_UCTENKA =	4	//účtenka
    public static final int DOK_VYPZUCTU =	5	//výpis z účtu
    public static final int DOK_DODATEK =	6	//dodatek ke smlouvě
    public static final int DOK_PFAKTURA =	7	//promo faktura
    public static final int DOK_TECHZPR = 	8	//technická zpráva
    public static final int DOK_VYJADRENI =	9	//vyjádření
    public static final int DOK_OBRAZEK =	10	//obrázek
    public static final int DOK_DOPIS =	11	//Dopis
    public static final int DOK_ODHLASKA =	50	//odhlaška
    public static final int DOK_PRIHL =	2	//přihláška
    public static final int DOK_SMLVYP =	12	//smlouva o výpůjčce
    public static final int DOK_UNKNOWN =	99	//neznamy

    List<String> dokladyColumns = ['datum', 'datum_splatnosti', 'akce', 'dodavatel', 'ucet', 'vs', 'cena', 'komu', 'stav', 'obsah', 'poznamka', 'doctype', 'id']

    NetadminConnector(Sql sql) {
        this.sql = sql
    }

    def updateDoklad(Doklad dok) {
        if (dok.akce == null) { dok.akce = 0 }
        if (dok.dodavatel == null) { dok.dodavatel = '-' }
        if (dok.ucet == null) { dok.ucet = '-' }
        if (dok.vs == null) { dok.vs = '-' }
        if (dok.komu == null) { dok.komu = 0 }

        if (dok.id != null) {
            String updateColumns = dokladyColumns.grep({ it != 'id' }).collect({ "$it = ?" }).join(', ')
            sql.executeUpdate("UPDATE doklady SET ${updateColumns} where id = ?", [
                    dateToString(dok.datum), dok.datumSplatnosti, dok.akce, dok.dodavatel, dok.ucet, dok.vs,
                    dok.cena, dok.komu, dok.stav, dok.obsah, dok.poznamka, dok.doctype, dok.id,])
        }
    }

    def insertDoklad(Doklad dok) {
        println("DEBUG: insert into doklad id = $dok.id")
        //not null: id datum akce dodavatel ucet vs komu stav obsah

        if (dok.akce == null) { dok.akce = 0 }
        if (dok.dodavatel == null) { dok.dodavatel = '-' }
        if (dok.ucet == null) { dok.ucet = '-' }
        if (dok.vs == null) { dok.vs = '-' }
        if (dok.komu == null) { dok.komu = 0 }
        dok.obsah = dok.obsah.replaceAll('–', '-')

        String insertColumns = dokladyColumns.collect().join(', ')
        String insertQMS = dokladyColumns.collect({ "?" }).join(', ') // question marks
        sql.executeUpdate("INSERT INTO doklady (${insertColumns}) VALUES (${insertQMS})".toString(), [
                dateToString(dok.datum), dok.datumSplatnosti, dok.akce, dok.dodavatel, dok.ucet, dok.vs,
                dok.cena, dok.komu, dok.stav, dok.obsah, dok.poznamka, dok.doctype, dok.id,])
    }

    def storeDokladOnFS(String name, byte[] data, boolean forceOverwrite = false) {

        def m = name =~ /^PF(\d+).20(\d+)$/
        if (m.matches()) {
            name = 'F' + m[0][2] + m[0][1];
        }
        m = name =~ /^ÚČT(\d+).20(\d+)$/
        if (m.matches()) {
            name = 'U' + m[0][2] + m[0][1];
        }
        m = name =~ /^Z(\d+).20(\d+)$/
        if (m.matches()) {
            name = 'Z' + m[0][2] + m[0][1];
        }

        File dir = null
        [dokladBaseDir, name.substring(0,3), name.substring(3,5)].each { dirName->
            dir = dir == null?new File(dirName):new File(dir, dirName)
            if (! dir.exists()) dir.mkdir()
        }
        File file = new File(dir, "${name}.pdf".toString());

        if (file.exists() && forceOverwrite) { file.delete() }

        if (!file.exists()) {
            file << data
        }
    }

    List<Doklad> selectDokladyByIds(List<String> ids) {
        def subsets = ids.collate( ids.size().intdiv( 500 ) )
        def result = []
        subsets.each { subids ->
            String condition = subids.collect({"?"}).join(", ")
            sql.eachRow("select * from doklady where id in ($condition)", subids) { row ->
                result.add(new Doklad(
                        id: row.ID, datum: asDate(row.DATUM), datumSplatnosti: row.DATUM_SPLATNOSTI, akce: row.AKCE, dodavatel: row.DODAVATEL, ucet: row.UCET,
                        vs: row.VS, cena: row.CENA, komu: row.KOMU, stav: row.STAV, obsah: row.OBSAH, poznamka: row.POZNAMKA, doctype: row.DOCTYPE,
                ))
            }
        }
        return result.size() > 0?result:null
    }

    Map<String, List<Rozpis>> selectRozpisyByDokladyIds(List<String> ids) {
        def subsets = ids.collate( ids.size().intdiv( 500 ) )
        Map<String, List<Rozpis>> result = [:]
        subsets.each { subids ->
            String condition = subids.collect({"?"}).join(", ")
            sql.eachRow("select * from rozpisy where dokladid in ($condition)", subids) { row ->
                String key = row.DOKLADID
                List<Rozpis> rozpisy = result.get(key)
                if (rozpisy == null) {
                    rozpisy = []
                    result.put(key, rozpisy)
                }
                rozpisy.add(new Rozpis(
                        id: row.ID, dokladId: row.DOKLADID, cena: row.CENA, vs: row.VS,
                        obsah: row.OBSAH, umisteniId: row.UMISTENIID, souvztaznostId: row.SOUVZTAZNOSTID,
                ))
            }
        }
        return result.size() > 0?result:null
    }

    def saveRozpis(Rozpis roz) {
        // load rozpis
        // if exists update
        // else insert
    }

    def selectAkceByYear(int year) {
        def list = []
        sql.eachRow("SELECT * FROM akce WHERE datum_schvaleni LIKE ?", ["%${year}".toString()]) { row ->
            list.add(new Akce(
                    id: row.id,
                    sekceId: row.sekceid,
                    nazev: row.nazev,
                    stav: row.stav,
                    obsah: row.obsah,
                    datumSchvaleni: asDate(row.datum_schvaleni),
                    datumUkonceni: asDate(row.datum_ukonceni),
                    userId: row.userid,
                    cena: asDecimal(row.cena)
            ))
        }
        return list
    }

    def selectSekce() {
        def list = []
        sql.eachRow("SELECT * FROM sekce") { row ->
            list.add(new Sekce(
                    id: row.id,
                    nazev: row.nazev,
                    group: row.group
            ))
        }
        return list
    }

    def upsertAkce(Akce akce) {
        if (akce.id == null) {
            akce.id = sql.firstRow(genNextval(),[Akce.tableName]).V as Long
        }

        sql.executeUpdate(genUpsert(Akce.tableName, Akce.columns),
                [ akce.id, akce.sekceId, akce.nazev, akce.stav, akce.obsah, akce.datumSchvaleni, akce.datumUkonceni, akce.userId, akce.cena, akce.schvaleno, akce.ukonceno, akce.smlouvanutna,
                  akce.sekceId, akce.nazev, akce.stav, akce.obsah, akce.datumSchvaleni, akce.datumUkonceni, akce.userId, akce.cena, akce.schvaleno, akce.ukonceno, akce.smlouvanutna])
    }

    def upsertSekce(Sekce s) {
        if (s.id == null) {
            s.id = sql.firstRow(genNextval(),[Sekce.tableName]).V as Long
        }
        sql.executeUpdate(genUpsert(Sekce.tableName, Sekce.columns), [ s.id, s.nazev, s.group, s.nazev, s.group ])
    }

    static String genUpsert(def table, def columns, def idColumn = 'id') {
        String insertColumns = columns.collect().join(', ')
        String insertQMS = columns.collect({ "?" }).join(', ') // question marks
        String updateQMS = columns.grep({ it != idColumn }).collect({ "$it = ?" }).join(', ') // question marks
        return "INSERT INTO $table ($insertColumns) VALUES ($insertQMS) ON DUPLICATE KEY UPDATE $updateQMS".toString()
    }

    static String genNextval() {
        return "SELECT Auto_increment as V FROM information_schema.tables WHERE table_name= ?".toString()
    }

    static fmt = new SimpleDateFormat("dd.MM.yyyy")

    def static asDate(String dateString) {
        if (notEmpty(dateString)) {
            return fmt.parse(dateString)
        }
        return null
    }

    def static dateToString(Date d) {
        d==null?null:fmt.format(d)
    }
}


