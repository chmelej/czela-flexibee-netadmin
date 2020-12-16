import groovy.sql.Sql
import net.czela.common.Helper

Sql sql = Helper.newSqlInstance("app.properties", this)

/**
 * Pro primy pristup do mysql
 *
 * generuje predpisy prispevku pro vsechny cleny.
 *
 * na zaklade workflow logu najde lidi kteri byli v dany mesic cleny a tem vygeneruje zaznam do deniku
 * mesic pro ktery se generuji prispevky je v argumentu
 *
 * - pousti se to kazdy den, takze nikdo neunikne.
 * - pokud se to nespusti pres mesic takto bude pruser.
 */
def version = '3.0'
Calendar now = new GregorianCalendar()

int year = now.get(Calendar.YEAR)
int month = now.get(Calendar.MONTH)

String notes = "$year/$month | generuj_prispevky.pl v$version | ${System.currentTimeMillis()}"
String charset = Helper.get("charset")

// init db
sql.execute("SET CHARACTER SET $charset")

// najdi cleny kteri byli dany mesic alespon chvilku pripojeny
String query = "SELECT DISTINCT u.vs, u.login, u.jmeno, u.prijmeni FROM workflow_logs wf, users u WHERE wf.obj_id = u.id AND "
query += getSqlCondition(year, month)

sql.withTransaction {
    sql.eachRow(query) { row ->
        // predepis prispevek
        String iquery = getSqlContribution(vs, row.jmeno, row.prijmeni, year, month, notes)
        sql.executeUpdate(iquery)
    }
}

/**
 * Vrati SQL podminku ktera z workflow vybere aktualni cleny v danem mesici
 * getSqlCondition(year, month);
 */
static String getSqlCondition(int year, int month) {
    def begin = String.format("'%04d-%02d-01 00:00:00'", year, month)
    month++
    if (month > 12) {
        month -= 12
        year++
    } 
    def end = String.format("'%04d-%02d-01 00:00:00'", year, month)

    def time_cond = "(((wf.from_date <= $begin) AND (wf.to_date >= $end)) OR (($begin <= wf.from_date) AND (wf.from_date < $end)) OR (($begin < wf.to_date) AND (wf.to_date <= $end)))"
    // nebudu predepisovat predpis tam kde uz existuje. orientuji se na zaklade datumu vlozeni!
    def hotovo = " vs not in (SELECT vs FROM denik WHERE cena = 350 AND md = 315000 AND d = 684000 AND str_to_date(datum, '%d.%m.%Y') >= $begin AND str_to_date(datum, '%d.%m.%Y') < $end)"

    def members_cond = "wf.wf_name = 'users' AND wf.status = 2"
    return "$members_cond AND $time_cond AND $hotovo\n"
}

/**
 * Vrati SQL insert pro predpis clenskeho prispevku
 * getSqlContribution(vs, login, fname, sname, year, month);
 */
static String getSqlContribution(int vs, String fname, String sname, int year, int month, String notes) {
    def xdate = String.format("'01.%02d.%04d'", month, year)
    def msg = "'$sname $fname - předpis členského příspěvku za $year/$month'"
    return "INSERT INTO denik (datum, MD, D, vs, obsah, cena, pozn) VALUES ($xdate, 315000, 684000, $vs, $msg, 350, '$notes');"
}
