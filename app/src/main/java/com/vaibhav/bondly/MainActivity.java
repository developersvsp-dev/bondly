package com.vaibhav.bondly;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.*;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private ProductDetails productDetails;

    // Subscription product ID from Play Console
    private static final String SUBSCRIPTION_PRODUCT_ID = "monthly_premium";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buyBtn = findViewById(R.id.btnBuyPremium);

        // -------------------------------
        // 1️⃣ Setup Billing Client
        // -------------------------------
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts() // or enableSubscriptionProducts() for clarity
                                .build()
                )
                .build();

        // -------------------------------
        // 2️⃣ Start Billing Connection
        // -------------------------------
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("IAP", "Billing connected!");
                    querySubscriptionProduct();
                } else {
                    Log.e("IAP", "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e("IAP", "Billing disconnected!");
            }
        });

        // -------------------------------
        // 3️⃣ Buy Button Click
        // -------------------------------
        buyBtn.setOnClickListener(v -> {
            if (productDetails != null) {
                startSubscriptionPurchase();
            } else {
                Toast.makeText(this, "Subscription not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------------------
    // 4️⃣ Query Subscription Product
    // -------------------------------
    private void querySubscriptionProduct() {

        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {

            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e("IAP", "Query failed: " + billingResult.getDebugMessage());
                return;
            }

            List<ProductDetails> list = result.getProductDetailsList();

            if (list.isEmpty()) {
                Log.e("IAP", "❌ Subscription not found. Check Play Console product ID.");
                return;
            }

            productDetails = list.get(0);
            Log.d("IAP", "Subscription fetched: " + productDetails.getName());
        });
    }

    // -------------------------------
    // 5️⃣ Start Subscription Purchase Flow
    // -------------------------------
    private void startSubscriptionPurchase() {

        // For subscriptions, we need the offer token from the base plan
        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList =
                productDetails.getSubscriptionOfferDetails();

        if (offerDetailsList == null || offerDetailsList.isEmpty()) {
            Log.e("IAP", "No subscription offer found!");
            return;
        }

        ProductDetails.SubscriptionOfferDetails offerDetails = offerDetailsList.get(0);

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerDetails.getOfferToken())
                        .build();

        BillingFlowParams flowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                        .build();

        BillingResult result = billingClient.launchBillingFlow(this, flowParams);
        Log.d("IAP", "Subscription purchase launched: " + result.getDebugMessage());
    }

    // -------------------------------
    // 6️⃣ Purchases Updated Listener
    // -------------------------------
    private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {

                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.e("IAP", "User canceled purchase");
            } else {
                Log.e("IAP", "Purchase failed: " + billingResult.getDebugMessage());
            }
        }
    };

    // -------------------------------
    // 7️⃣ Handle Purchase & Acknowledge
    // -------------------------------
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Toast.makeText(MainActivity.this, "Subscription active!", Toast.LENGTH_SHORT).show();
                        Log.d("IAP", "Purchase acknowledged");
                    }
                });
            }
        }
    }
}
