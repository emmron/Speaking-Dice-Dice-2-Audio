package com.emmetthoolahan.dicetospeech;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Arrays;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    public static final String PRODUCT_REMOVE_ADS    = "remove_ads";
    public static final String PRODUCT_PREMIUM_TEAMS = "premium_teams_pack";
    public static final String PRODUCT_SEASON_PASS   = "season_pass";

    private static final String PREF_NAME          = "f1_purchases";
    private static final String KEY_ADS_REMOVED    = "ads_removed";
    private static final String KEY_PREMIUM_TEAMS  = "premium_teams";
    private static final String KEY_SEASON_PASS    = "season_pass_owned";

    private final BillingClient billingClient;
    private final SharedPreferences prefs;
    private final PurchaseListener listener;

    public interface PurchaseListener {
        void onAdsRemoved();
        void onPremiumTeamsUnlocked();
        void onSeasonPassUnlocked();
    }

    public BillingManager(Context context, PurchaseListener listener) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.listener = listener;
        this.billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {}
        });
    }

    public boolean isAdsRemoved()         { return prefs.getBoolean(KEY_ADS_REMOVED,   false); }
    public boolean isPremiumTeamsUnlocked() { return prefs.getBoolean(KEY_PREMIUM_TEAMS, false); }
    public boolean isSeasonPassOwned()    { return prefs.getBoolean(KEY_SEASON_PASS,    false); }

    /** Launch a Google Play billing flow for the given product ID. */
    public void launchPurchase(Activity activity, String productId) {
        if (!billingClient.isReady()) return;
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Arrays.asList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                )).build();
        billingClient.queryProductDetailsAsync(params, (result, productDetailsList) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && !productDetailsList.isEmpty()) {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Arrays.asList(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetailsList.get(0))
                                        .build()
                        )).build();
                activity.runOnUiThread(() -> billingClient.launchBillingFlow(activity, flowParams));
            }
        });
    }

    private void checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (result, purchases) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : purchases) handlePurchase(purchase);
                    }
                }
        );
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) handlePurchase(purchase);
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams ack = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken()).build();
            billingClient.acknowledgePurchase(ack, r -> {});
        }
        SharedPreferences.Editor ed = prefs.edit();
        List<String> products = purchase.getProducts();
        if (products.contains(PRODUCT_REMOVE_ADS)) {
            ed.putBoolean(KEY_ADS_REMOVED, true).apply();
            if (listener != null) listener.onAdsRemoved();
        }
        if (products.contains(PRODUCT_PREMIUM_TEAMS)) {
            ed.putBoolean(KEY_PREMIUM_TEAMS, true).apply();
            if (listener != null) listener.onPremiumTeamsUnlocked();
        }
        if (products.contains(PRODUCT_SEASON_PASS)) {
            ed.putBoolean(KEY_SEASON_PASS, true).apply();
            if (listener != null) listener.onSeasonPassUnlocked();
        }
    }

    public void destroy() {
        if (billingClient != null) billingClient.endConnection();
    }
}
