import groovy.sql.Sql
import net.czela.common.Helper

Sql sql = Helper.newSqlInstance("app.properties", this)

sql.executeUpdate("UPDATE denik SET datum_date = STR_TO_DATE(datum, '%d.%m.%Y') WHERE datm_date is null AND datum rlike '^[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}'".toString())
sql.executeUpdate("UPDATE akce SET cena = '0' WHERE cena not rlike '^\\-?[0-9]+(.[0-9]+)?\$'".toString())
