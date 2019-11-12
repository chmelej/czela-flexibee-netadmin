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
}
