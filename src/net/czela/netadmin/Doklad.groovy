package net.czela.netadmin

class Doklad {
    String id
    Date datum              // VARCHAR
    Date datumSplatnosti
    Long akce
    String dodavatel
    String ucet
    Long vs
    BigDecimal cena
    Long komu
    Long stav
    String obsah
    String poznamka
    Long doctype

    List<Rozpis> rozpisy = []


    @Override
    public String toString() {
        return "Doklad{" +
                "id='" + id +
                ", datum=" + datum +
                ", datumSplatnosti=" + datumSplatnosti +
                ", akce=" + akce +
                ", dodavatel='" + dodavatel +
                ", ucet=" + ucet +
                ", vs=" + vs +
                ", cena=" + cena +
                ", komu=" + komu +
                ", stav=" + stav +
                ", obsah='" + obsah +
                ", poznamka='" + poznamka +
                ", doctype=" + doctype +
                '}'
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Doklad doklad = (Doklad) o

        if (akce != doklad.akce) return false
        if (cena != doklad.cena) return false
        if (datum != doklad.datum) return false
        if (datumSplatnosti != doklad.datumSplatnosti) return false
        if (doctype != doklad.doctype) return false
        if (dodavatel != doklad.dodavatel) return false
        if (id != doklad.id) return false
        if (komu != doklad.komu) return false
        if (obsah != doklad.obsah) return false
        if (poznamka != doklad.poznamka) return false
        if (rozpisy != doklad.rozpisy) return false
        if (stav != doklad.stav) return false
        if (ucet != doklad.ucet) return false
        if (vs != doklad.vs) return false

        return true
    }
}
