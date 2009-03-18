package com.bradmcevoy.http;

import com.bradmcevoy.common.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Date;
import sun.nio.cs.ThreadLocalCoders;

public class Utils {

    private final static char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static Resource findChild(Resource parent, Path path) {
        return _findChild(parent, path.getParts(), 0);
    }

    private static Resource _findChild(Resource parent, String[] arr, int i) {
        if (parent instanceof CollectionResource) {
            CollectionResource col = (CollectionResource) parent;
            String childName = arr[i];

            Resource child = col.child(childName);
            if (child == null) {
                return null;
            } else {
                if (i < arr.length - 1) {
                    return _findChild(child, arr, i + 1);
                } else {
                    return child;
                }
            }
        } else {
            return null;
        }
    }

    public static Date now() {
        return new Date();
    }

    public static Date addSeconds(Date dt, long seconds) {
        return addSeconds(dt, (int) seconds);
    }

    public static Date addSeconds(Date dt, int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    public static String getProtocol(String url) {
        String protocol = url.substring(0, url.indexOf(":"));
        return protocol;
    }

    /*
     * Provided by Kenneth V - see http://www.ettrema.com:8080/browse/MIL-31
     * 
     *
     *
     */
    public static String encodeHref(String s) {
        if (s.startsWith("https")) {
            s = s.substring(6);
            return encodeUrl(s, "https");
        } else {
            s = s.substring(5);
            return encodeUrl(s, "http");
        }

    }

    /**
     * This encodes all ampersands so will not work with request parameters
     *
     * @param s
     * @param scheme
     * @return
     */
    public static String encodeUrl(String s, String scheme) {
        try {
            URI uri = new URI(scheme, s, null);
            s = uri.toASCIIString();
            s = s.replaceAll("&", "%26");
            return s;
        } catch (URISyntaxException ex) {
            throw new RuntimeException(s, ex);
        }
    }

    public static String escapeXml(String s) {
        s = s.replaceAll("\"", "&quot;");
        s = s.replaceAll("&", "&amp;");
        s = s.replaceAll("'", "&apos;");
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    /**
     * this is a modified verion of java.net.URI.encode(s)
     *
     * the java.net version only encodes characters over \u0080, but this
     * version also applies encoding to characters below char 48
     *
     * this method should be applied only to parts of a URL, not the whole
     * URL as forward slashes, semi-colons etc will be encoded
     *
     * @param s
     * @return
     */
    public static String percentEncode(String s) {
        int n = s.length();
        if (n == 0) {
            return s;
        }

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080' || s.charAt(i) <= (char)48) {
                break;
            }
            if (++i >= n) {
                return s;
            }
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8").encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80 || b <= (char)48) {
                appendEscape(sb, (byte) b);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

}