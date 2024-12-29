package com.turtrack.server.service.turtrack;

import com.turtrack.server.config.StripeConfig;
import com.turtrack.server.dto.turtrack.ProductWithPricesDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ProductService {

    private final StripeConfig stripeConfig;

    public List<ProductWithPricesDTO> getAllProducts() throws StripeException {
        // First, get all active products
        ProductListParams params = ProductListParams.builder()
                .setLimit(100L)
                .setActive(true)
                .addExpand("data.default_price")
                .build();

        ProductCollection productCollection = Product.list(params);
        List<Product> products = new ArrayList<>(productCollection.getData());
        List<ProductWithPricesDTO> productsWithPrices = new ArrayList<>();

        // Fetch prices for each product
        for (Product product : products) {
            PriceListParams priceParams = PriceListParams.builder()
                    .setProduct(product.getId())
                    .setActive(true)
                    .build();

            List<Price> prices = Price.list(priceParams).getData();

            // Add to our list of products with prices
            productsWithPrices.add(ProductWithPricesDTO.builder()
                    .product(product)
                    .prices(prices)
                    .build());
        }

        return productsWithPrices;
    }

    public ProductWithPricesDTO getProduct(String productId) throws StripeException {
        // Retrieve product with expanded default price
        Map<String, Object> retrieveParams = new HashMap<>();
        retrieveParams.put("expand", List.of("default_price"));
        Product product = Product.retrieve(productId, retrieveParams, null);

        // Fetch all prices for this product
        PriceListParams priceParams = PriceListParams.builder()
                .setProduct(productId)
                .setActive(true)
                .build();

        List<Price> prices = Price.list(priceParams).getData();

        return ProductWithPricesDTO.builder()
                .product(product)
                .prices(prices)
                .build();
    }
}

