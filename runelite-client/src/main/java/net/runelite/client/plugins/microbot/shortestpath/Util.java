package net.runelite.client.plugins.microbot.shortestpath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Util {
    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (true) {
            int read = in.read(buffer, 0, buffer.length);

            if (read == -1) {
                return result.toByteArray();
            }

            result.write(buffer, 0, read);
        }
    }

    public static int[] concatenate(int[][] arrays) {
        int length = Arrays.stream(arrays).mapToInt(array -> array == null ? 0 : array.length).sum();
        int[] result = new int[length];
        int offset = 0;
        for (int[] array : arrays) {
            if (array == null) {
                continue;
            }
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
