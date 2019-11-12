import groovy.sql.Sql

Sql sql = Helper.newSqlInstance("app.properties", this)

sql.eachRow("select * from soubor") { row ->
    def id = row.ID
    def fileName = row.JMENO_SOUBORU
    println(fileName)
    def f = new File("doklady/${id}_${fileName}".replaceAll(/[^0-9A-Za-z_\/.-]+/,'_'))
    if (f.exists()) f.delete()
    f << row.DATA
}
