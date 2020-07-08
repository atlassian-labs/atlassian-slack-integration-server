package com.atlassian.plugins.slack.api.client;

import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * This was borrowed from OkHttp's MockServer test implementations to support testing HTTPS:
 * https://github.com/duego/android-okhttp/blob/master/mockwebserver/src/main/java/com/squareup/okhttp/internal/SslContextBuilder.java
 *
 * <p>
 * Constructs an SSL context for testing. This uses Bouncy Castle to generate a
 * self-signed certificate for a single hostname such as "localhost".
 *
 * <p>
 * The crypto performed by this class is relatively slow. Clients should
 * reuse SSL context instances where possible.
 */
public final class SslContextBuilder {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final long ONE_DAY_MILLIS = 1000L * 60 * 60 * 24;
    private final String hostName;
    private long notBefore = System.currentTimeMillis();
    private long notAfter = System.currentTimeMillis() + ONE_DAY_MILLIS;

    /**
     * @param hostName the subject of the host. For TLS this should be the
     *                 domain name that the client uses to identify the server.
     */
    public SslContextBuilder(String hostName) {
        this.hostName = hostName;
    }

    public SSLContext build() throws GeneralSecurityException {
        char[] password = "password".toCharArray();

        // Generate public and private keys and use them to make a self-signed certificate.
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = selfSignedCertificate(keyPair);

        // Put 'em in a key store.
        KeyStore keyStore = newEmptyKeyStore(password);
        Certificate[] certificateChain = {certificate};
        keyStore.setKeyEntry("private", keyPair.getPrivate(), password, certificateChain);
        keyStore.setCertificateEntry("cert", certificate);

        // Wrap it up in an SSL context.
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
                new SecureRandom());
        return sslContext;
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Generates a certificate for {@code hostName} containing {@code keyPair}'s
     * public key, signed by {@code keyPair}'s private key.
     */
    @SuppressWarnings("deprecation") // use the old Bouncy Castle APIs to reduce dependencies.
    private X509Certificate selfSignedCertificate(KeyPair keyPair) throws GeneralSecurityException {
        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new X500Principal("CN=" + hostName),
                BigInteger.ONE,
                new Date(notBefore),
                new Date(this.notAfter),
                new X500Principal("CN=" + hostName),
                keyPair.getPublic());

        try {
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .build(keyPair.getPrivate());

            return new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(contentSigner));
        } catch (OperatorCreationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
