package org.gtri.hdap.mdata.common.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionMetaData {
    private String shimmerId;
    public SessionMetaData() {
        this.shimmerId = "";
    }
    public SessionMetaData(String shimmerId) {
        this.shimmerId = shimmerId;
    }
    public String getShimmerId() {
        return this.shimmerId;
    }
    public void setShimmerId(String shimmerId) {
        this.shimmerId = shimmerId;
    }
}
