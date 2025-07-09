package com.medco.HealthConnectProvider.utils.Image;

import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ImageUtils {

    public static byte[] compressImage(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[5*1024];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        while (!deflater.finished()) {
            int compressedDataLength = deflater.deflate(buffer);
            outputStream.write(buffer, 0, compressedDataLength);
        }

        outputStream.close();


        return outputStream.toByteArray();
    }

    public static byte[] decompressImage(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] tmp = new byte[5*1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(tmp);
                outputStream.write(tmp, 0, count);
            }
            outputStream.close();
        } catch (Exception ignored) {
        }


        return outputStream.toByteArray();
    }

    public static String sanitizeImageName(String originalFilename) {

        String extension = FilenameUtils.getExtension(originalFilename);

        String sanitizedFilename = originalFilename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .trim();

        if (sanitizedFilename.length() > 240) {
            sanitizedFilename = sanitizedFilename.substring(0, 240);
        }

        String uniqueId = UUID.randomUUID().toString();


        String newFilename = String.format("%s_%s.%s",
                FilenameUtils.getBaseName(sanitizedFilename),
                uniqueId,
                extension);

        return newFilename.toLowerCase();

    }
}
