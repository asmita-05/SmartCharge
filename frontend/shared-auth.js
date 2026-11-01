(() => {
    const tokenKey = "smartchargeAccessToken";
    const roleKeys = new Set(["smartchargeUser", "smartchargeAdmin", "smartchargeOperator"]);
    const nativeGet = Storage.prototype.getItem;
    const nativeSet = Storage.prototype.setItem;
    const nativeRemove = Storage.prototype.removeItem;

    // Keep each tab's selected identity separate, while preserving localStorage
    // as a one-time compatibility source for sessions created before this change.
    Storage.prototype.getItem = function (key) {
        if (this === window.localStorage && roleKeys.has(String(key))) {
            const tabValue = window.sessionStorage.getItem(key);
            if (tabValue !== null) return tabValue;
        }
        return nativeGet.call(this, key);
    };
    Storage.prototype.setItem = function (key, value) {
        if (this === window.localStorage && roleKeys.has(String(key))) {
            window.sessionStorage.setItem(key, value);
        }
        return nativeSet.call(this, key, value);
    };
    Storage.prototype.removeItem = function (key) {
        if (this === window.localStorage && roleKeys.has(String(key))) {
            window.sessionStorage.removeItem(key);
        }
        return nativeRemove.call(this, key);
    };

    const originalFetch = window.fetch.bind(window);
    window.fetch = (input, init = {}) => {
        const rawUrl = typeof input === "string" || input instanceof URL ? input : input.url;
        let url;
        try { url = new URL(rawUrl, window.location.href); }
        catch { return originalFetch(input, init); }

        const isApi = url.pathname.startsWith("/api/")
            && (url.origin === window.location.origin
                || (url.hostname === "localhost" && url.port === "8080")
                || (url.hostname === "127.0.0.1" && url.port === "8080"));
        const isPublicAuth = /\/api\/users\/(login|register|verify(?:\/resend)?)$/.test(url.pathname);
        const token = window.sessionStorage.getItem(tokenKey);
        if (!isApi || isPublicAuth || !token) return originalFetch(input, init);

        const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
        headers.set("Authorization", `Bearer ${token}`);
        return originalFetch(input, { ...init, headers, credentials: "omit" });
    };

    const path = window.location.pathname;
    const roleFolder = path.match(/\/(user|admin|operator)\//)?.[1];
    const isLoginPage = /\/login\.html$/.test(path);
    if (roleFolder && !isLoginPage && !window.sessionStorage.getItem(tokenKey)) {
        window.location.replace("login.html");
    }
})();
