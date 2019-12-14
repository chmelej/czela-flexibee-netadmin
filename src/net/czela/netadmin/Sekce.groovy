package net.czela.netadmin

class Sekce {
    static final String[] columns = 'id,nazev,group'.split(',')
    static final String tableName = 'sekce'
    static final String pkName = 'id'

    Long id
    String nazev
    String group


    @Override
    public String toString() {
        return "Sekce{" +
                "id=" + id +
                ", nazev='" + nazev + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}
