package com.timecapsule.security;

import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Proves AES-GCM round-trips, fresh IVs, and tamper rejection. */
class CryptoServiceTest {

    private SecretKey key() throws Exception {
        return CryptoService.deriveKey("correct horse".toCharArray(), CryptoService.newSalt());
    }

    @Test
    void textRoundTrip() throws Exception {
        SecretKey key = key();
        CryptoService.Sealed sealed = CryptoService.encryptText("dear future me…", key);
        assertEquals("dear future me…", CryptoService.decryptText(sealed, key));
    }

    @Test
    void identicalEntriesEncryptDifferently() throws Exception {
        SecretKey key = key();
        CryptoService.Sealed a = CryptoService.encryptText("same words", key);
        CryptoService.Sealed b = CryptoService.encryptText("same words", key);
        assertFalse(Arrays.equals(a.iv(), b.iv()), "IVs must be fresh per entry");
        assertFalse(Arrays.equals(a.cipher(), b.cipher()));
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        SecretKey key = key();
        CryptoService.Sealed sealed = CryptoService.encryptText("sealed memory", key);
        byte[] broken = sealed.cipher().clone();
        broken[broken.length - 1] ^= 0x01;
        assertThrows(AEADBadTagException.class,
                () -> CryptoService.decrypt(new CryptoService.Sealed(sealed.iv(), broken), key));
    }

    @Test
    void wrongPasswordDerivesUnusableKey() throws Exception {
        byte[] salt = CryptoService.newSalt();
        SecretKey right = CryptoService.deriveKey("right-word".toCharArray(), salt);
        SecretKey wrong = CryptoService.deriveKey("wrong-word".toCharArray(), salt);
        CryptoService.Sealed sealed = CryptoService.encryptText("private", right);
        assertThrows(AEADBadTagException.class, () -> CryptoService.decrypt(sealed, wrong));
    }

    @Test
    void derivedKeysAreStable() throws Exception {
        byte[] salt = CryptoService.newSalt();
        SecretKey a = CryptoService.deriveKey("same".toCharArray(), salt);
        SecretKey b = CryptoService.deriveKey("same".toCharArray(), salt);
        assertArrayEquals(a.getEncoded(), b.getEncoded());
    }
}
