package com;

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
     * Reference: http://stackoverflow.com/questions/4113890/how-to-calculate-the-internet-checksum-from-a-byte-in-java.
     */
    public static long compChecksum(byte[] buf) {

        int length = buf.length;
        int i = 0;

        long sum = 0;
        long data = 0;
        while (length > 1) {
            data = (((buf[i]) << 8) | ((buf[i + 1]) & 0xFF));

            sum += data;
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }

            i += 2;
            length -= 2;
        }

        if (length > 0) {
            sum += (buf[i] << 8);

            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }

        sum = ~sum;
        sum = sum & 0xFFFF;
        return sum;

    }

}
