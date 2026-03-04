package com.kmoroz.stockalert.marketdata.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.kmoroz.stockalert.marketdata.exception.PriceNotFoundException;
import com.kmoroz.stockalert.marketdata.service.PriceService;

import java.util.Map;

import static com.kmoroz.stockalert.common.AlertSystemConstants.STOCK_PRICE;

@Controller("/prices")
@Tag(name = "Market Data", description = "Endpoints for retrieving real-time stock prices")
public class PriceController implements PriceApi {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @Get("/{symbol}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<Map<String, String>> getLatestPrice(String symbol) {
        String price = priceService.getLatestPrice(symbol)
                .orElseThrow(() -> new PriceNotFoundException(STOCK_PRICE + symbol + " not found"));
        return HttpResponse.ok(Map.of("price", price));
    }
}
