// File: app/src/main/java/com/vaibhav/bondly/BillingManager.java
package com.vaibhav.bondly;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.android.billingclient.api.*;
import java.util.Collections;
import java.util.List;

public class BillingManager {
    private static final String TAG = "BillingManager";
    private static BillingManager instance;
    private BillingClient billingClient;
    private static final String SUBSCRIPTION_ID = "monthly_premium";

    public interface SubscriptionCallback {
        void onCheckComplete(boolean isSubscribed);
    }

    private BillingManager(Context context) {
        initBillingClient(context);
    }

    public static BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context.getApplicationContext());
        }
        return instance;
    }

    private void initBillingClient(Context context) {
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing connected");
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected");
            }
        });
    }

    // 🔥 FIXED: Always ensure billingClient is ready BEFORE purchase
    public void launchSubscriptionPurchase(Activity activity, SubscriptionCallback callback) {
        Log.d(TAG, "🚀 Launching purchase flow...");

        if (billingClient == null) {
            Log.e(TAG, "❌ BillingClient null");
            Toast.makeText(activity, "Please restart app", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 CRITICAL FIX: Ensure client ready + fresh connection if needed
        BillingClientStateListener purchaseListener = new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ PURCHASE CLIENT READY - launching flow");
                    doLaunchPurchase(activity);
                } else {
                    Log.e(TAG, "❌ Purchase client failed: " + result.getDebugMessage());
                    Toast.makeText(activity, "Billing service unavailable", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "❌ Purchase client disconnected");
            }
        };

        if (billingClient.isReady()) {
            Log.d(TAG, "📡 Purchase client ready - launching");
            doLaunchPurchase(activity);
        } else {
            Log.d(TAG, "🔄 Purchase client reconnecting...");
            billingClient.startConnection(purchaseListener);
        }
    }

    private void doLaunchPurchase(Activity activity) {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build(),
                (result, productDetailsList) -> {
                    Log.d(TAG, "📦 Product query: " + result.getResponseCode());
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK &&
                            productDetailsList != null &&
                            !productDetailsList.getProductDetailsList().isEmpty()) {

                        ProductDetails productDetails = productDetailsList.getProductDetailsList().get(0);
                        List<ProductDetails.SubscriptionOfferDetails> offers =
                                productDetails.getSubscriptionOfferDetails();

                        if (offers != null && !offers.isEmpty()) {
                            BillingFlowParams.ProductDetailsParams productParams =
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(offers.get(0).getOfferToken())
                                            .build();

                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(Collections.singletonList(productParams))
                                    .build();

                            BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
                            Log.d(TAG, "💳 LAUNCH RESULT: " + launchResult.getResponseCode() + " - " + launchResult.getDebugMessage());
                        } else {
                            Log.e(TAG, "❌ No offers found");
                            Toast.makeText(activity, "Subscription not available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "❌ Product not found: " + SUBSCRIPTION_ID);
                        Toast.makeText(activity, "Product setup incomplete", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Keep your existing checkSubscriptionStatus() - it's perfect
    public void checkSubscriptionStatus(SubscriptionCallback callback) {
        Log.d(TAG, "🔍 Starting subscription check...");
        if (billingClient == null) {
            callback.onCheckComplete(false);
            return;
        }

        BillingClientStateListener checkListener = new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryPurchases(callback);
                } else {
                    callback.onCheckComplete(false);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                callback.onCheckComplete(false);
            }
        };

        if (billingClient.isReady()) {
            queryPurchases(callback);
        } else {
            billingClient.startConnection(checkListener);
        }
    }

    private void queryPurchases(SubscriptionCallback callback) {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            boolean isSubscribed = false;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED &&
                            purchase.getProducts().contains(SUBSCRIPTION_ID)) {
                        isSubscribed = true;
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase);
                        }
                        break;
                    }
                }
            }
            Log.d(TAG, "🎯 Subscription status: " + isSubscribed);
            callback.onCheckComplete(isSubscribed);
        });
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        Log.d(TAG, "🔔 Purchases updated: " + billingResult.getResponseCode());
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    };

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
            acknowledgePurchase(purchase);
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            Log.d(TAG, "✅ Acknowledged: " + (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK));
        });
    }
}
