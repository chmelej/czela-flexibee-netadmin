package net.czela.netadmin

class Akce {
    static final String[] columns = 'id,sekceid,nazev,stav,obsah,datum_schvaleni,datum_ukonceni,userid,cena,schvaleno,ukonceno,smlouvanutna'.split(',')
    static final tableName = 'akce'
    static final pkName = 'id'


    long id
    long sekceId = 0
    String nazev
    long stav = 1
    String obsah = '???'
    String datumSchvaleni = '01.01.2019'
    //Date datumSchvaleni = asDate('01.01.2019')
    String datumUkonceni = '31.12.2019'
    //Date datumUkonceni = asDate('31.12.2019')
    Long userId = 0
    BigDecimal cena = 0
    String schvaleno = '-'
    String ukonceno	= '-'
    // userid2	INT
    Integer smlouvanutna = 0

    @Override
    public String toString() {
        return "Akce{" +
                "id=" + id +
                ", sekceId=" + sekceId +
                ", nazev='" + nazev + '\'' +
                ", stav=" + stav +
                ", obsah='" + obsah + '\'' +
                ", datumSchvaleni=" + datumSchvaleni +
                ", datumUkonceni=" + datumUkonceni +
                ", userId=" + userId +
                ", cena=" + cena +
                '}';
    }
}
