package com.example.CustomerPortalBackend.constants;

import java.util.UUID;

public class AppConstants {
    public static final String PAGE_NUMBER = "0";
    public static final String PAGE_SIZE = "5";
    public static final String SORT_USER_BY = "email";
    public static final String SORT_DIR = "asc";

    public static final String[] AUTH_PUBLIC_URL = {
            "/admin/login",
            "/admin/refresh",
            "/hotels/seeAllHotels",
            "/hotels/seeAllRoomTypes",
            "/hotels/seeAllRatePlans",
            "/user/register",
            "/user/login",
            "/user/refresh",
            "/oauth2/authorization/google",
            "/login/oauth2/code/google"
    };

    public static UUID parseUUID(String uuid) {
        return UUID.fromString(uuid);
    }
}
