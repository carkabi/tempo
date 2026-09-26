package com.cappielloantonio.tempo.repository.peach;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class PeachSupportBillingManager
        implements PurchasesUpdatedListener {

    public static final String SUPPORT_1 = "peach_supporter_1";
    public static final String SUPPORT_3 = "peach_supporter_3";
    public static final String SUPPORT_5 = "peach_supporter_5";

    public interface Callback {
        void onProducts(List<SupportProduct> products);
        void onSupportState(boolean active, String productId);
        void onMessage(String message);
    }

    public static final class SupportProduct {
        private final String productId;
        private final String price;
        private final ProductDetails productDetails;
        private final String offerToken;

        SupportProduct(
                String productId,
                String price,
                ProductDetails productDetails,
                String offerToken
        ) {
            this.productId = productId;
            this.price = price;
            this.productDetails = productDetails;
            this.offerToken = offerToken;
        }
        public String getProductId() {
            return productId;
        }

        public String getPrice() {
            return price;
        }

        ProductDetails getProductDetails() {
            return productDetails;
        }

        String getOfferToken() {
            return offerToken;
        }
    }

    private final BillingClient billingClient;
    private final Callback callback;
    private final Map<String, SupportProduct> products =
            new HashMap<>();

    public PeachSupportBillingManager(
            Context context,
            Callback callback
    ) {
        this.callback = callback;
        this.billingClient = BillingClient.newBuilder(
                        context.getApplicationContext()
                )
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();
    }
    public void start() {
        if (billingClient.isReady()) {
            refreshProducts();
            refreshSupportState();
            return;
        }

        billingClient.startConnection(
                new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(
                            @NonNull BillingResult billingResult
                    ) {
                        if (billingResult.getResponseCode()
                                == BillingClient.BillingResponseCode.OK) {
                            refreshProducts();
                            refreshSupportState();
                        } else {
                            callback.onMessage(
                                    "Google Play Billing indisponible."
                            );
                        }
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        callback.onMessage(
                                "Connexion Google Play interrompue."
                        );
                    }
                }
        );
    }

    public void close() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
    private void refreshProducts() {
        List<QueryProductDetailsParams.Product> requested =
                new ArrayList<>();

        requested.add(product(SUPPORT_1));
        requested.add(product(SUPPORT_3));
        requested.add(product(SUPPORT_5));

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(requested)
                        .build();

        billingClient.queryProductDetailsAsync(
                params,
                new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(
                            @NonNull BillingResult billingResult,
                            @NonNull QueryProductDetailsResult result
                    ) {
                        if (billingResult.getResponseCode()
                                != BillingClient.BillingResponseCode.OK) {
                            callback.onProducts(
                                    Collections.emptyList()
                            );
                            return;
                        }

                        products.clear();
                        List<SupportProduct> available =
                                new ArrayList<>();

                        for (ProductDetails details
                                : result.getProductDetailsList()) {
                            SupportProduct support =
                                    map(details);

                            if (support != null) {
                                products.put(
                                        support.getProductId(),
                                        support
                                );
                                available.add(support);
                            }
                        }

                        callback.onProducts(available);
                    }
                }
        );
    }
    private QueryProductDetailsParams.Product product(
            String productId
    ) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(
                        BillingClient.ProductType.SUBS
                )
                .build();
    }

    private SupportProduct map(ProductDetails details) {
        List<ProductDetails.SubscriptionOfferDetails> offers =
                details.getSubscriptionOfferDetails();

        if (offers == null || offers.isEmpty()) {
            return null;
        }

        ProductDetails.SubscriptionOfferDetails offer =
                offers.get(0);

        List<ProductDetails.PricingPhase> phases =
                offer.getPricingPhases()
                        .getPricingPhaseList();

        if (phases.isEmpty()) {
            return null;
        }

        ProductDetails.PricingPhase recurring =
                phases.get(phases.size() - 1);

        return new SupportProduct(
                details.getProductId(),
                recurring.getFormattedPrice(),
                details,
                offer.getOfferToken()
        );
    }
    public void purchase(
            Activity activity,
            String productId
    ) {
        SupportProduct product = products.get(productId);

        if (product == null) {
            callback.onMessage(
                    "Cette formule n’est pas encore disponible."
            );
            return;
        }

        BillingFlowParams.ProductDetailsParams detailsParams =
                BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(
                                product.getProductDetails()
                        )
                        .setOfferToken(
                                product.getOfferToken()
                        )
                        .build();

        BillingFlowParams params =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                Collections.singletonList(
                                        detailsParams
                                )
                        )
                        .build();

        billingClient.launchBillingFlow(activity, params);
    }

    public void refreshSupportState() {
        if (!billingClient.isReady()) {
            return;
        }

        QueryPurchasesParams params =
                QueryPurchasesParams.newBuilder()
                        .setProductType(
                                BillingClient.ProductType.SUBS
                        )
                        .build();
        billingClient.queryPurchasesAsync(
                params,
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode()
                            != BillingClient.BillingResponseCode.OK) {
                        callback.onSupportState(false, null);
                        return;
                    }

                    boolean active = false;
                    String activeProduct = null;

                    for (Purchase purchase : purchases) {
                        if (purchase.getPurchaseState()
                                == Purchase.PurchaseState.PURCHASED) {
                            active = true;

                            if (!purchase.getProducts().isEmpty()) {
                                activeProduct =
                                        purchase.getProducts().get(0);
                            }

                            acknowledgeIfNeeded(purchase);
                        }
                    }

                    callback.onSupportState(
                            active,
                            activeProduct
                    );
                }
        );
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }

        AcknowledgePurchaseParams params =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(
                                purchase.getPurchaseToken()
                        )
                        .build();

        billingClient.acknowledgePurchase(
                params,
                result -> {
                    if (result.getResponseCode()
                            == BillingClient.BillingResponseCode.OK) {
                        refreshSupportState();
                    }
                }
        );
    }
    @Override
    public void onPurchasesUpdated(
            @NonNull BillingResult billingResult,
            List<Purchase> purchases
    ) {
        if (billingResult.getResponseCode()
                == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState()
                        == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeIfNeeded(purchase);
                }
            }
            refreshSupportState();
            return;
        }

        if (billingResult.getResponseCode()
                != BillingClient.BillingResponseCode.USER_CANCELED) {
            callback.onMessage(
                    "Le paiement n’a pas pu être finalisé."
            );
        }
    }
}
