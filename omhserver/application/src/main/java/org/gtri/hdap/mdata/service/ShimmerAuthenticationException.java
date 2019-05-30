package org.gtri.hdap.mdata.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by es130 on 9/12/2018.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ShimmerAuthenticationException extends RuntimeException {
    public ShimmerAuthenticationException(String message) {
        super(message);
    }
}
