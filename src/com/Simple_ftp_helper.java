package com;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Created by Muchen on 4/10/16.
 */
public class Simple_ftp_helper {

    /**
     *
     * @param buf
     * @return
     *
     * This helper function implements Checksum(16bits) computation.
     * Reference:   http://stackoverflow.com/questions/4113890/how-to-calculate-the-internet-checksum-from-a-byte-in-java.
     *              https://docs.oracle.com/javase/8/docs/technotes/guides/io/example/Sum.java
     * MX
     */
    public static byte[] compChecksum(byte[] buf) {

        ByteBuffer bb = ByteBuffer.wrap(buf);

        int sum = 0;

        while (bb.hasRemaining()) {
            if ((sum & 1) != 0)
                sum = (sum >> 1) + 0x8000;
            else
                sum >>= 1;
            sum += bb.get() & 0xff;
            sum &= 0xffff;
        }

        byte[] res = new byte[2];
        byte[] tmp = new BigInteger(Integer.toString(sum), 10).toByteArray();

        System.arraycopy(tmp, 0, res, 2 - tmp.length, tmp.length);
        return res;

//        /**
//         * Another Way to compute the checksum.
//         */
//        int length = buf.length;
//        int i = 0;
//
//        long sum = 0;
//        long data = 0;
//        while (length > 1) {
//            data = (((buf[i]) << 8) | ((buf[i + 1]) & 0xFF));
//
//            sum += data;
//            if ((sum & 0xFFFF0000) > 0) {
//                sum = sum & 0xFFFF;
//                sum += 1;
//            }
//
//            i += 2;
//            length -= 2;
//        }
//
//        if (length > 0) {
//            sum += (buf[i] << 8);
//
//            if ((sum & 0xFFFF0000) > 0) {
//                sum = sum & 0xFFFF;
//                sum += 1;
//            }
//        }
//
//        sum = ~sum;
//        sum = sum & 0xFFFF;
//        return (int)sum;

    }

}
