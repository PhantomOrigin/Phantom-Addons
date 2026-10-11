package com.phantomaddons.utils;

import com.phantomaddons.PhantomAddons;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class PhantomApi {

    public static final String PRIMARY = "https://api.phantomaddonsapi.com";
    public static final String LEGACY = "https://kuudra-profile-worker-v2.phantomaddons.workers.dev";

    private static final long PRIMARY_RETRY_MS = 5 * 60 * 1000L;
    private static volatile long primaryFailedAt = 0;

    private PhantomApi() {}

    @FunctionalInterface
    public interface Call<T> {
        T run(String baseUrl) throws IOException;
    }

    public static final class HttpException extends IOException {
        public final int status;
        public final String code;

        public HttpException(int status, String message, String code) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    // ── Credentials ──────────────────────────────────────────────────────────

    public static void authorize(HttpURLConnection conn) {
        String bearer = PhantomAuth.bearer();
        if (bearer != null) conn.setRequestProperty("Authorization", "Bearer " + bearer);
    }

    public static <T> T authed(Call<T> call) throws IOException {
        if (PhantomAuth.bearer() == null) PhantomAuth.ensureLogin();
        if (PhantomAuth.bearer() == null) {
            throw new IOException(PhantomAuth.isDisabled()
                    ? "Signing in with your Minecraft account is currently disabled."
                    : "Couldn't sign in with your Minecraft account. Check your connection and try again.");
        }
        try {
            return withFallback(call);
        } catch (HttpException e) {
            if ("session_disabled".equals(e.code)) {
                PhantomAuth.markDisabled();
            } else if ("token_invalid".equals(e.code)) {
                PhantomAuth.invalidate();
                if (PhantomAuth.relogin()) return withFallback(call);
            }
            throw e;
        }
    }

    public static <T> T withFallback(Call<T> call) throws IOException {
        boolean primarySkipped = System.currentTimeMillis() - primaryFailedAt < PRIMARY_RETRY_MS;
        if (!primarySkipped) {
            try {
                return call.run(PRIMARY);
            } catch (IOException e) {
                if (!couldNotConnect(e)) throw e;
                primaryFailedAt = System.currentTimeMillis();
                PhantomAddons.LOGGER.warn("[PhantomAddons] {} unreachable ({}), using the legacy address", PRIMARY, e.toString());
            }
        }
        return call.run(LEGACY);
    }

    private static boolean couldNotConnect(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UnknownHostException || t instanceof ConnectException || t instanceof NoRouteToHostException
                    || t instanceof javax.net.ssl.SSLException) return true;
            if (t instanceof SocketTimeoutException && t.getMessage() != null
                    && t.getMessage().toLowerCase().contains("connect")) return true;
        }
        return false;
    }
}
