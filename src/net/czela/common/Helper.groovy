package net.czela.common

import groovy.sql.Sql

class Helper {
    static Properties props

/**
 * Connects to the database
 */
    static Sql newSqlInstance(String propFile, Object script, String prefix = null) {
        String name = script.getClass().getName() + ".groovy"
        props = new Properties()
        def dirs = ['./', '../','../../../']
        boolean found = false
        for(def d:dirs) {
            File f = new File(d + propFile)
            if (f.exists()) {
                props.load(new FileReader(f))
                found = true
                break;
            }
        }
        if (!found) {
            System.err.println("Properties file ${propFile} cannot be found.")
        }

        if (prefix) {
            prefix += "."
        } else {
            prefix = ""
        }

        Properties jdbcProps = new Properties()
        jdbcProps.setProperty("user", props.get("${prefix}jdbc.username".toString()) as String)
        jdbcProps.setProperty("password", props.get("${prefix}jdbc.password".toString()) as String)
        jdbcProps.setProperty('ApplicationName', name)// pgsql
        return (Sql) Sql.newInstance(props.get("${prefix}jdbc.url".toString()), jdbcProps, props.get("${prefix}jdbc.driver".toString()))
    }

    static String get(String property) {
        return props.get(property)
    }

    static String get(String property, String defaultValue) {
        String v = props.get(property)
        return v == null?defaultValue:v
    }

    static def asLong(String longString) {
        if (notEmpty(longString)) {
            return Long.parseLong(longString)
        }
        return null
    }

    static boolean notEmpty(String s) {
        s != null && s.length() > 0;
    }

    static BigDecimal asDecimal(String decString) {
        if (notEmpty(decString)) {
            try {
                return new BigDecimal(decString)
            } catch (NumberFormatException e) {
                println("bad number = '$decString'");
                throw e
            }
        }
        return null
    }

    static String filterNumbersOnly(String s) {
        s==null?null:s.replaceAll(/[^0-9]/,'').trim()
    }
}