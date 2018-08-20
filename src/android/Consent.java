package ch.herrmanntech.cordova.consent;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.google.ads.consent.*;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Consent extends CordovaPlugin {

	private static final String TAG = "Consent";

	private Activity context;
	private ConsentForm form;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		this.context = cordova.getActivity();

		Log.d(TAG, "initialized consent plugin");
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		if (action.equals("verifyConsent")) {
			JSONArray submittedPublisherIds = args.getJSONArray(0);
			String[] publisherIds = new String[submittedPublisherIds.length()];
			for (int i = 0; i < submittedPublisherIds.length(); i++) {
				publisherIds[0] = submittedPublisherIds.getString(i);
			}

			verifyConsent(callbackContext, publisherIds, args.getString(1), args.getBoolean(2), args.getBoolean(3));
		} else if (action.equals("changeConsentDecision")) {
			final String privacyPolicyUrl = args.getString(0);
			final boolean showProVersionOption = args.getBoolean(1);

			final CallbackContext cb = callbackContext;
			this.context.runOnUiThread(new Runnable() {
				public void run() {
					askForConsent(cb, privacyPolicyUrl, showProVersionOption);
				}
			});
		}

		return true;
	}

	private void verifyConsent(final CallbackContext cb, final String[] publisherIds, final String privacyPolicyUrl, final boolean showProVersionOption, final boolean isDebug) {
		ConsentInformation consentInformation = ConsentInformation.getInstance(context);
		checkDebug(isDebug, consentInformation);

		consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
			@Override
			public void onConsentInfoUpdated(ConsentStatus consentStatus) {

				//First, lets see if we're in the EEA...
				boolean isInEea = consentInformation.isRequestLocationInEeaOrUnknown();

				if (consentStatus == ConsentStatus.UNKNOWN && !isInEea) {
					JSONObject resultNotInEea = new JSONObject();
					try {
						resultNotInEea.put("consent", "UNKNOWN");
						resultNotInEea.put("isAdFree", false);
						resultNotInEea.put("hasShownDialog", false);
						resultNotInEea.put("isNotInEea", true);
						cb.success(resultNotInEea);
					} catch(JSONException ex) {
						cb.error(ex.getMessage());
					}
				} else if (consentStatus == ConsentStatus.PERSONALIZED) {
					JSONObject resultPersonalized = new JSONObject();
					try {
						resultPersonalized.put("consent", "PERSONALIZED");
						resultPersonalized.put("hasShownDialog", false);
						resultPersonalized.put("isNotInEea", false);
						cb.success(resultPersonalized);
					} catch (JSONException ex) {
						cb.error(ex.getMessage());
					}

				} else if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
					JSONObject resultNonPersonalized = new JSONObject();
					try {
						resultNonPersonalized.put("consent", "NON_PERSONALIZED");
						resultNonPersonalized.put("hasShownDialog", false);
						resultNonPersonalized.put("isNotInEea", false);
						cb.success(resultNonPersonalized);
					} catch (JSONException ex) {
						cb.error(ex.getMessage());
					}

				} else if (consentStatus == ConsentStatus.UNKNOWN) {
					askForConsent(cb, privacyPolicyUrl, showProVersionOption);
				}
			}

			@Override
			public void onFailedToUpdateConsentInfo(String errorDescription) {
				Log.d(TAG, "callback error method");
				cb.error(errorDescription);
			}
		});
	}

	private void checkDebug(final boolean isDebug, final ConsentInformation consentInformation) {
		if (isDebug) {
			consentInformation.addTestDevice(getHashedDeviceId());
			consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
		} else {
			consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_DISABLED);
		}
	}

	private void askForConsent(final CallbackContext cb, final String privacyPolicyUrl, final boolean showProVersionOption) {
		URL privacyUrl = null;
		try {
			privacyUrl = new URL(privacyPolicyUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			cb.error("malformed privacy policy url");
		}

		ConsentForm.Builder builder = new ConsentForm.Builder(context, privacyUrl)
			.withListener(new ConsentFormListener() {
				@Override
				public void onConsentFormLoaded() {
					// Consent form loaded successfully.
					showConsentForm();
				}

				@Override
				public void onConsentFormOpened() {
					// Consent form was displayed.
				}

				@Override
				public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
					// Consent form was closed.
					JSONObject resultAskConsent = new JSONObject();
					try {
						String consent = null;
						if (consentStatus == ConsentStatus.PERSONALIZED) {
							consent = "PERSONALIZED";
						} else if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
							consent = "NON_PERSONALIZED";
						} else if (consentStatus == ConsentStatus.UNKNOWN) {
							consent = "UNKNOWN";
						}

						resultAskConsent.put("consent", consent);
						resultAskConsent.put("isAdFree", userPrefersAdFree);
						resultAskConsent.put("hasShownDialog", true);
						resultAskConsent.put("isNotInEea", false);

						cb.success(resultAskConsent);
					} catch (JSONException ex) {
						cb.error(ex.getMessage());
					}
				}

				@Override
				public void onConsentFormError(String errorDescription) {
					// Consent form error.
					cb.error(errorDescription);
				}
			})
			.withPersonalizedAdsOption()
			.withNonPersonalizedAdsOption();

		if (showProVersionOption) {
			builder.withAdFreeOption();
		}

		form = builder.build();
		form.load();

	}

	private void showConsentForm() {
		form.show();
	}

	private String getHashedDeviceId() {
		ContentResolver contentResolver = context.getContentResolver();
		String androidId =
			contentResolver == null
				? null
				: Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
		return md5(((androidId == null) || isEmulator()) ? "emulator" : androidId);
	}

	private String md5(String string) {
		// Old devices have a bug where OpenSSL can leave MessageDigest in a bad state, but trying
		// multiple times seems to clear it.
		for (int i = 0; i < 3 /** max attempts */; ++i) {
			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(string.getBytes());
				return String.format(Locale.US, "%032X", new BigInteger(1, md5.digest()));
			} catch (NoSuchAlgorithmException e) {
				// Try again.
			} catch (ArithmeticException ex) {
				return null;
			}
		}
		return null;
	}

	private boolean isEmulator() {
		return Build.FINGERPRINT.startsWith("generic")
			|| Build.FINGERPRINT.startsWith("unknown")
			|| Build.MODEL.contains("google_sdk")
			|| Build.MODEL.contains("Emulator")
			|| Build.MODEL.contains("Android SDK built for x86")
			|| Build.MANUFACTURER.contains("Genymotion")
			|| (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
			|| "google_sdk".equals(Build.PRODUCT);
	}

}