package net.czela.flexibee.https

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class AnyHostVerifier implements HostnameVerifier {
    @Override
    boolean verify(String hostname, SSLSession sslSession) {
        return true
    }
}
