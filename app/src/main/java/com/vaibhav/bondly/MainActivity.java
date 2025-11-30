package com.vaibhav.bondly;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private ProductDetails productDetails;

    private static final String PRODUCT_ID = "monthly_premium";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buyBtn = findViewById(R.id.btnBuyPremium);

        // -------------------------------
        // 1️⃣ Setup Billing Client
        // -------------------------------
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .setListener(purchasesUpdatedListener)
                .build();

        // -------------------------------
        // 2️⃣ Start Billing Connection
        // -------------------------------
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("IAP", "Billing connected!");
                    queryProduct();
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
                startPurchase();
            } else {
                Toast.makeText(this, "Product not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------------------
    // 4️⃣ Query Product
    // -------------------------------
    private void queryProduct() {

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(BillingResult billingResult, QueryProductDetailsResult result) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && result.getProductDetailsList() != null
                        && !result.getProductDetailsList().isEmpty()) {

                    productDetails = result.getProductDetailsList().get(0);
                    Log.d("IAP", "Product fetched: " + productDetails.getName());

                } else {
                    Log.e("IAP", "❌ Product not found on Play Console!");
                }
            }
        });
    }

    // -------------------------------
    // 5️⃣ Start Purchase Flow
    // -------------------------------
    private void startPurchase() {

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build();

        BillingFlowParams flowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(List.of(productDetailsParams))
                        .build();

        BillingResult result = billingClient.launchBillingFlow(this, flowParams);
        Log.d("IAP", "Purchase launched: " + result.getDebugMessage());
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
                        Toast.makeText(MainActivity.this, "Premium Unlocked!", Toast.LENGTH_SHORT).show();
                        Log.d("IAP", "Purchase acknowledged");
                    }
                });
            }
        }
    }
}
