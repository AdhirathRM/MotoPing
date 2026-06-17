package com.example.motoping;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

public class R2Uploader {

    // --- YOUR CLOUDFLARE R2 CREDENTIALS ---
    private static final String R2_ACCESS_KEY = "ac41fa70a0698d5a294dda863a1a7b99";
    private static final String R2_SECRET_KEY = "240dfe5ad8e7b74001d69f843e1a4ec3360589d276bf004e8545bad945bbe33a";
    private static final String R2_ENDPOINT = "https://95ecb4b784c88bade8931f4959aeb943.r2.cloudflarestorage.com";
    private static final String BUCKET_NAME = "motoping-glovebox";

    // If you enable public access on your bucket, put the public URL here to view images later
    private static final String R2_PUBLIC_URL_PREFIX = "https://pub-37a8f87f68b24e319614f22da4a58600.r2.dev/";

    public interface Callback {
        void onSuccess(String url);
        void onError(String error);
    }

    public static void uploadDocument(Context context, Uri fileUri, String mimeType, Callback callback) {
        new Thread(() -> {
            try {
                ClientConfiguration clientConfig = new ClientConfiguration();
                clientConfig.setSignerOverride("AWSS3V4SignerType");

                AWSCredentials credentials = new BasicAWSCredentials(R2_ACCESS_KEY, R2_SECRET_KEY);
                AmazonS3Client s3Client = new AmazonS3Client(credentials, clientConfig);

                s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
                s3Client.setEndpoint(R2_ENDPOINT);
                s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());

                String extension = mimeType.contains("pdf") ? ".pdf" : ".jpg";
                File tempFile = new File(context.getCacheDir(), UUID.randomUUID().toString() + extension);

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();

                String objectKey = "documents/" + UUID.randomUUID().toString() + extension;

                Date expiration = new Date();
                long expTimeMillis = expiration.getTime() + (1000 * 60 * 15);
                expiration.setTime(expTimeMillis);

                GeneratePresignedUrlRequest generatePresignedUrlRequest =
                        new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey)
                                .withMethod(HttpMethod.PUT)
                                .withExpiration(expiration);

                URL presignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

                HttpURLConnection connection = (HttpURLConnection) presignedUrl.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", mimeType);

                OutputStream out = connection.getOutputStream();
                FileInputStream in = new FileInputStream(tempFile);
                byte[] buf = new byte[4096];
                int count;
                while ((count = in.read(buf)) > 0) {
                    out.write(buf, 0, count);
                }
                out.close();
                in.close();

                int responseCode = connection.getResponseCode();
                tempFile.delete();

                if (responseCode == 200 || responseCode == 204) {
                    String finalUrl = R2_PUBLIC_URL_PREFIX + objectKey;
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalUrl));
                } else {
                    throw new Exception("Cloudflare rejected upload. HTTP Code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("R2Uploader", "Upload failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    // NEW: Generates a self-destructing link to view locked files
    public static void getSecureReadUrl(String storedDbUrl, Callback callback) {
        new Thread(() -> {
            try {
                // Extract just the filename from the old Firebase URL
                String objectKey;
                if (storedDbUrl.contains("documents/")) {
                    objectKey = storedDbUrl.substring(storedDbUrl.indexOf("documents/"));
                } else {
                    throw new Exception("Invalid file path");
                }

                ClientConfiguration clientConfig = new ClientConfiguration();
                clientConfig.setSignerOverride("AWSS3V4SignerType");

                AWSCredentials credentials = new BasicAWSCredentials(R2_ACCESS_KEY, R2_SECRET_KEY);
                AmazonS3Client s3Client = new AmazonS3Client(credentials, clientConfig);

                s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
                s3Client.setEndpoint(R2_ENDPOINT);
                s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());

                // Create a VIP Pass that expires in exactly 60 seconds
                Date expiration = new Date();
                long expTimeMillis = expiration.getTime() + (1000 * 60);
                expiration.setTime(expTimeMillis);

                GeneratePresignedUrlRequest generatePresignedUrlRequest =
                        new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey)
                                .withMethod(HttpMethod.GET)
                                .withExpiration(expiration);

                URL secureTempUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(secureTempUrl.toString()));

            } catch (Exception e) {
                Log.e("R2Uploader", "Link generation failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}