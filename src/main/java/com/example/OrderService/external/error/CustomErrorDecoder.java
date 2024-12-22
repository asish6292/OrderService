package com.example.OrderService.external.error;

import com.example.OrderService.exception.CustomException;
import com.example.OrderService.external.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;

@Log4j2
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("Error****** ::{}", response.request().url());
            log.info("Error****** ::{}", response.request().headers());
            ErrorResponse errorResponse = objectMapper.readValue(response.body().asInputStream(), ErrorResponse.class);

            return new CustomException(errorResponse.getErrorMessage(), errorResponse.getErrorCode(), response.status());
        } catch (IOException e) {
            throw new CustomException("Internal server error", "INTERNAL_SERVER_ERROR", response.status());
        }
    }
}
