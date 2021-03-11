package net.czela.netadmin

class User {
    static final String[] columns = 'id,vs,jmeno,prijmeni,login,mesto,adresa,psc,mobil,telefon,email'.split(',')
    static final String tableName = 'users'
    static final String pkName = 'id'

    Long id, vs
    String jmeno, prijmeni, login, mesto, adresa, psc, mobil, telefon, email


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", vs=" + vs +
                ", jmeno='" + jmeno + '\'' +
                ", prijmeni='" + prijmeni + '\'' +
                ", login='" + login + '\'' +
                ", mesto='" + mesto + '\'' +
                ", adresa='" + adresa + '\'' +
                ", psc='" + psc + '\'' +
                ", mobil='" + mobil + '\'' +
                ", telefon='" + telefon + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
