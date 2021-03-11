import groovy.sql.Sql
import net.czela.common.Helper
import java.text.SimpleDateFormat

/*
    kdyz se posere nacini dat z banky, je potrebato rucne upravit. Tohle by melo pomoct. plus sahodlouhej popis od Filipa
 */

Sql sql = Helper.newSqlInstance("app.properties", this)

// vstupni soubor ziskas na http://kazbunda:8080/fio/vypis?od=2021-02-18&do=2021-02-19, pak se pripadnemusi rucne upravit
def vypisFile = new File(args[0])
def rawXmlData = new String(vypisFile.readBytes())

def slurper = new XmlSlurper();
def data = slurper.parseText(rawXmlData)
def fmt = new SimpleDateFormat("yyyy-MM-ddX")

String cisloVypisu = "${data.Info.idFrom.text()}-${data.Info.idTo.text()}"
Date obdobiOd = fmt.parse(data.Info.dateStart.text())
Date obdobiDo = fmt.parse(data.Info.dateEnd.text())
Date vlozeno = new Date()
BigDecimal pocatecniZustatek = new BigDecimal(data.Info.openingBalance.text())
BigDecimal koncovyZustatek = new BigDecimal(data.Info.closingBalance.text())

println("Z Info jsem nacet :" + [cisloVypisu, obdobiOd, obdobiDo, vlozeno, pocatecniZustatek, koncovyZustatek])

BigDecimal obrat = koncovyZustatek.subtract(pocatecniZustatek)
BigDecimal sum = BigDecimal.ZERO
def ids = []
data.TransactionList.Transaction.each { t ->
    def obj = new BigDecimal(t.column_1.text() as String)
    sum = sum.add(obj)
    ids.add(new BigInteger(t.column_22.text() as String))
}
assert sum.equals(obrat)
println("${cisloVypisu} Obrat v Info a suma transakci sedi na chlup ${sum}")

/*
sum = BigDecimal.ZERO
String cond = ids.collect({"?"}).join(", ")
sql.eachRow("SELECT porad_cislo, datum, vs, castka, zprava FROM parsovane_vypisy where porad_cislo in ($cond)".toString(), ids) { row ->
    println("Duplicitni zaznam: $row")
}
assert ! sum.equals(BigDecimal.ZERO)
println("Zadna duplicita to vypada dobre!")
 */

/*
sql.executeUpdate("INSERT INTO uploadovane_vypisy (cislo_vypisu, obdobi_od, obdobi_do, vlozeno, exportovano, " +
        "pocatecni_zustatek, koncovy_zustatek, banka_id, vypis) VALUES (?,?,?,?,0,?,?,2,?)".toString(),
        [cisloVypisu, obdobiOd, obdobiDo, vlozeno, pocatecniZustatek, koncovyZustatek, rawXmlData] )
*/
def denPred = obdobiDo.toLocalDateTime().minusDays(2).toDate()
def denPo = obdobiDo.toLocalDateTime().plusDays(2).toDate()
println("Kontrola obdobi od $denPred do $denPo")
sql.eachRow("CALL kontrola_vypisu(?, ?, ?)", [2, denPred, denPo]) { row ->
    println(row.CISLO_VYPISU)
}
println("Vypada to OK")
// http://kazbunda:8080/parse?id=13480
