import groovy.sql.Sql
import net.czela.common.Helper

import java.text.SimpleDateFormat

def fmt = new SimpleDateFormat("yyyy-MM-dd")
def fmt2 = new SimpleDateFormat("yyyy/MM")

Date date = (args.size() > 0)?fmt.parse(args[0]):new Date()

Sql sql = Helper.newSqlInstance("app.properties", this)

/**
 * Pro primy pristup do mysql
 *
 * generuje predpisy prispevku pro vsechny cleny.
 *
 * na zaklade workflow logu najde lidi kteri byli v dany mesic cleny a tem vygeneruje zaznam do tabulky cleni_prispevky
 * datum pro ktery se generuji prispevky je v argumentu ve tvaru 'YYYY-MM-DD' pricemz den se ignoruje
 * splatnost se predpoklada k 15. dni nasledujiciho mesice
 *
 * - pousti se to kazdy den, takze nikdo neunikne.
 * - pokud se to nespusti pres mesic takto bude pruser.
 * - pokud chybi argument bere se aktualni datum
 *
 * create table cleni_prispevky (
 * 	id 					        INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
 * 	vs 					        SMALLINT UNSIGNED NOT NULL,
 * 	castka 				        DECIMAL(10,2),
 * 	ucto_doklad_id 		        VARCHAR(50),		                -- kod dokladu  z ucto
 * 	popisek 				    VARCHAR(255),
 * 	datum_vytvorenni 		    TIMESTAMP NOT NULL,             -- kdy jsme vygenerovali tento radek
 * 	datum_vystaveni_dokladu 	DATE NULL DEFAULT NULL, 	    -- kdy jsme vygenerovali doklad v ucto
 * 	datum_zaplaceni		        DATE NULL DEFAULT NULL, 	    -- kdy se zparovala platba v ucto
 * 	datum_splatnosti 		    DATE NOT NULL,	                -- kdy ma byt zaplaceno (15. pristi mesic)
 * 	datum_prispevku		        DATE NOT NULL                   -- 1. den mesice pro ktery plati prispevek
 * )
 */
Calendar cal = new GregorianCalendar()
cal.setTime(date)

String datumVPopisku = fmt2.format(date)

cal.set(Calendar.DAY_OF_MONTH, 1)
Date firstDate = cal.getTime();

cal.add(Calendar.MONTH, 1)
cal.add(Calendar.DAY_OF_MONTH, -1)
Date lastDate = cal.getTime();

cal.add(Calendar.MONTH, 1)
cal.set(Calendar.DAY_OF_MONTH, 15)
Date datumSplatnosti = cal.getTime()

Date datumVytvoreni = new Date()
int prispevekCastka = 350
int stavClen = 2

// najdi cleny kteri byli dany mesic alespon chvilku pripojeny
String query = """SELECT DISTINCT u.vs, u.login, u.jmeno, u.prijmeni FROM workflow_logs wf, users u 
    WHERE wf.obj_id = u.id AND wf.wf_name = 'users' AND wf.status = ?
    AND ((wf.from_date <= date(?) AND wf.to_date >= date(?)) OR (date(?) <= wf.from_date AND wf.from_date < date(?))
      OR (date(?) < wf.to_date AND wf.to_date <= date(?)))
    AND not exists (SELECT 1 FROM cleni_prispevky cp WHERE cp.castka = ? AND cp.datum_prispevku = date(?) AND u.vs = cp.vs)"""

int cnt = 0;
sql.withBatch(1000, "INSERT INTO cleni_prispevky (vs, castka, popisek, datum_vytvorenni, datum_splatnosti, datum_prispevku)" +
        " VALUES (?, ?, ?, ?, ?, ?)") { stmt ->
    sql.eachRow(query, [stavClen, firstDate, lastDate, firstDate, lastDate, firstDate, lastDate, prispevekCastka, firstDate]) { row ->
        String popisek = "${row.prijmeni} ${row.jmeno} - předpis členského příspěvku za $datumVPopisku"
        stmt.addBatch([row.VS as int, prispevekCastka, popisek, datumVytvoreni.toTimestamp(), datumSplatnosti.toTimestamp(), firstDate.toTimestamp()])
        cnt++
    }
}
println("Bylo vegenerovano $cnt prispevku za obdobi zacinajici ${fmt.format(firstDate)}")
