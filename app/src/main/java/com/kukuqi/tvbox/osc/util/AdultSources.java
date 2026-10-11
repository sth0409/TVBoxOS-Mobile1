package com.kukuqi.tvbox.osc.util;

import android.util.Base64;

import com.kukuqi.tvbox.osc.bean.Subscription;
import com.orhanobut.hawk.Hawk;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Access to the two bundled adult subscriptions lasts only for this process. */
public final class AdultSources {
    public static final String R18 = "assets://subscriptions/r18.json";
    public static final String CN18 = "assets://subscriptions/18cn.json";
    private static final String PASSWORD_SALT = "adult_sources_password_salt";
    private static final String PASSWORD_HASH = "adult_sources_password_hash";
    private static final int ITERATIONS = 120000;
    private static volatile boolean visible;

    private AdultSources() {}

    public static boolean isProtected(String url) {
        return R18.equals(url) || CN18.equals(url);
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean show) {
        visible = show;
    }

    public static boolean hasPassword() {
        return !Hawk.get(PASSWORD_SALT, "").isEmpty() && !Hawk.get(PASSWORD_HASH, "").isEmpty();
    }

    public static boolean setPassword(String password) {
        if (password == null || password.length() < 4) return false;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        try {
            String hash = Base64.encodeToString(derive(password, salt), Base64.NO_WRAP);
            Hawk.put(PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP));
            Hawk.put(PASSWORD_HASH, hash);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean verifyPassword(String password) {
        if (!hasPassword() || password == null) return false;
        try {
            byte[] salt = Base64.decode((String) Hawk.get(PASSWORD_SALT, ""), Base64.DEFAULT);
            byte[] expected = Base64.decode((String) Hawk.get(PASSWORD_HASH, ""), Base64.DEFAULT);
            return MessageDigest.isEqual(expected, derive(password, salt));
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static List<Subscription> visibleItems(List<Subscription> items) {
        List<Subscription> result = new ArrayList<>();
        for (Subscription item : items) {
            if (visible || !isProtected(item.getUrl())) result.add(item);
        }
        return result;
    }

    /** Keep the stored selection and checked flags away from hidden sources. */
    public static String safeSelection(List<Subscription> items, String selectedUrl) {
        if (visible || !isProtected(selectedUrl)) return selectedUrl;
        String replacement = null;
        for (Subscription item : items) {
            if (SourceDefaults.VOD.equals(item.getUrl())) { replacement = item.getUrl(); break; }
            if (replacement == null && !isProtected(item.getUrl())) replacement = item.getUrl();
        }
        if (replacement == null) replacement = SourceDefaults.VOD;
        for (Subscription item : items) item.setChecked(replacement.equals(item.getUrl()));
        return replacement;
    }

    public static void resetHiddenSelection() {
        List<Subscription> items = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());
        String selected = Hawk.get(HawkConfig.API_URL, "");
        if (!isProtected(selected)) return;
        String safe = safeSelection(items, selected);
        Hawk.put(HawkConfig.API_URL, safe);
        Hawk.put(HawkConfig.SUBSCRIPTIONS, items);
    }
}
